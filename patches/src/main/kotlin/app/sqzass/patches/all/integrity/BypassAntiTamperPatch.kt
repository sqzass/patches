package app.sqzass.patches.all.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.shared.forceInvokeResult
import app.sqzass.patches.shared.methodReferenceOf
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private val logger = Logger.getLogger("BypassAntiTamperPatch")

private const val PACKAGE_MANAGER = "Landroid/content/pm/PackageManager;"
private const val PACKAGE_INFO = "Landroid/content/pm/PackageInfo;"
private const val SIGNATURE_TYPE = "Landroid/content/pm/Signature;"
private const val MESSAGE_DIGEST = "Ljava/security/MessageDigest;"
private const val VENDING = "com.android.vending"

// SIGNATURE_MATCH — returned when two packages are signed by the same cert.
private const val SIGNATURE_MATCH = 0

private val SIGNATURE_CHECK_METHODS = setOf("checkSignatures", "checkUidSignatures")

// ponytail: digest checks split across helper methods or verified in native
// code are not matched; getInstallSourceInfo results cannot be fabricated
// without an extension (only getInstallerPackageName and the PackageInfo
// field are forced).
private fun isInstallerNameGet(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass == PACKAGE_MANAGER &&
        reference.name == "getInstallerPackageName" &&
        reference.returnType == "Ljava/lang/String;"
}

private fun isInstallerNameField(instruction: Instruction): Boolean {
    val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference ?: return false
    return reference.definingClass == PACKAGE_INFO &&
        reference.name == "installerPackageName" &&
        reference.type == "Ljava/lang/String;"
}

private fun isSignatureCompare(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass == PACKAGE_MANAGER &&
        reference.name in SIGNATURE_CHECK_METHODS &&
        reference.returnType == "I"
}

/**
 * Whether a boolean method digests package signatures itself — the shape of a
 * self-contained signature digest check (MessageDigest.getInstance plus a
 * Signature or signing info reference).
 */
private fun isSignatureDigestCheck(method: Method): Boolean {
    if (method.returnType != "Z") return false
    val instructions = method.implementation?.instructions ?: return false
    var hasDigest = false
    var hasSignature = false
    for (instruction in instructions) {
        when (val reference = (instruction as? ReferenceInstruction)?.reference) {
            is MethodReference -> {
                if (reference.definingClass == MESSAGE_DIGEST && reference.name == "getInstance") hasDigest = true
                if (reference.definingClass == SIGNATURE_TYPE) hasSignature = true
            }
            is FieldReference -> {
                if (reference.definingClass == PACKAGE_INFO &&
                    reference.name in setOf("signatures", "signingInfo")
                ) {
                    hasSignature = true
                }
            }
            else -> Unit
        }
        if (hasDigest && hasSignature) return true
    }
    return false
}

@Suppress("unused")
val bypassAntiTamperPatch = bytecodePatch(
    name = "Bypass anti-tamper checks",
    description = "Defeats common anti-tamper checks: reports the Play Store as installer, forces signature comparisons to report a match, and can make signature digest methods report an intact app.",
    default = false,
) {
    category("Integrity")

    val spoofInstallerOption = booleanOption(
        key = "spoofInstallerPackage",
        default = true,
        title = "Spoof Installer Package",
        description = "Report com.android.vending as the installer in getInstallerPackageName and PackageInfo reads.",
    )
    val forceSignatureChecksOption = booleanOption(
        key = "forceSignatureChecks",
        default = true,
        title = "Force Signature Checks",
        description = "Force PackageManager.checkSignatures and checkUidSignatures to report SIGNATURE_MATCH.",
    )
    val bypassDigestChecksOption = booleanOption(
        key = "bypassDigestChecks",
        default = false,
        title = "Force Digest Checks Valid",
        description = "Make boolean signature digest methods return true. Assumes such checks return true when the app is intact.",
    )

    execute {
        val spoofInstaller = spoofInstallerOption.value == true
        val forceSignatureChecks = forceSignatureChecksOption.value == true
        val bypassDigest = bypassDigestChecksOption.value == true
        if (!spoofInstaller && !forceSignatureChecks && !bypassDigest) {
            logger.info("All options disabled.")
            return@execute
        }

        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.let { raw ->
                    val instructions = raw.toList()
                    (spoofInstaller && (
                        instructions.any(::isInstallerNameGet) ||
                            instructions.any(::isInstallerNameField)
                        )) ||
                        (forceSignatureChecks && instructions.any(::isSignatureCompare)) ||
                        (bypassDigest && isSignatureDigestCheck(method))
                } == true
            },
        ) { instructions ->
            var count = 0

            if (spoofInstaller) {
                for (index in instructions.indices) {
                    val instruction = instructions[index]
                    if (isInstallerNameGet(instruction) &&
                        forceInvokeResult(instructions, index) { register -> "const-string v$register, \"$VENDING\"" }
                    ) {
                        count++
                    } else if (isInstallerNameField(instruction) && instruction is TwoRegisterInstruction) {
                        replaceInstruction(index, "const-string v${instruction.registerA}, \"$VENDING\"")
                        count++
                    }
                }
            }

            if (forceSignatureChecks) {
                for (index in instructions.indices) {
                    if (isSignatureCompare(instructions[index]) &&
                        forceInvokeResult(instructions, index) { register -> "const/4 v$register, $SIGNATURE_MATCH" }
                    ) {
                        count++
                    }
                }
            }

            if (bypassDigest && isSignatureDigestCheck(this)) {
                // Descending so the inserted return is never revisited.
                for (index in instructions.indices.reversed()) {
                    val instruction = instructions.getOrNull(index) ?: continue
                    if (instruction.opcode == Opcode.RETURN && instruction is OneRegisterInstruction) {
                        replaceInstruction(index, "const/4 v${instruction.registerA}, 0x1")
                        addInstruction(index + 1, "return v${instruction.registerA}")
                        count++
                    }
                }
            }

            count
        }

        logger.info("Applied $patched anti-tamper bypass replacements.")
    }
}
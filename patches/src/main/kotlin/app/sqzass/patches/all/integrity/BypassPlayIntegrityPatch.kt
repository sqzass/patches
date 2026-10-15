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
import java.util.logging.Logger

private val logger = Logger.getLogger("BypassPlayIntegrityPatch")

private const val INTEGRITY_MARKER = "integrity"

// Boolean verdict accessors forced to true when the aggressive toggle is on.
private val VERDICT_METHODS = setOf(
    "isAllowed", "isGenuine", "isValid", "isSuccess", "isSuccessful",
    "meetsDeviceIntegrity", "meetsBasicIntegrity", "meetsStrongIntegrity",
)

// ponytail: integrity tokens are validated server-side and cannot be forged
// offline; this only rewrites client-side verdict reads. Obfuscated verdict
// methods with unrelated names are missed; upgrade path is tracing the
// IntegrityTokenResponse.token call chain.
private fun isIntegrityClass(type: String): Boolean =
    type.lowercase().contains(INTEGRITY_MARKER)

private fun isVerdictRead(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return isIntegrityClass(reference.definingClass) &&
        (reference.name in VERDICT_METHODS || reference.name.startsWith("is") && reference.name.endsWith("Allowed")) &&
        reference.returnType == "Z"
}

private fun isVerdictMethod(method: Method): Boolean =
    method.returnType == "Z" &&
        isIntegrityClass(method.definingClass) &&
        (method.name in VERDICT_METHODS || method.name.startsWith("is") && method.name.endsWith("Allowed"))

@Suppress("unused")
val bypassPlayIntegrityPatch = bytecodePatch(
    name = "Bypass Play Integrity",
    description = "Experimental: forces client-side Play Integrity verdict checks to report success. Integrity tokens validated on a server cannot be forged offline, so server-gated features stay gated.",
    default = false,
) {
    category("Integrity")

    val forceVerdictReadsOption = booleanOption(
        key = "forceVerdictReads",
        default = true,
        title = "Force Verdict Reads",
        description = "Force Play Integrity verdict getter results to report success.",
    )
    val forceVerdictMethodsOption = booleanOption(
        key = "forceVerdictMethods",
        default = false,
        title = "Force Verdict Methods",
        description = "Make Play Integrity verdict methods return true. Assumes such checks return true when the device passes.",
    )

    execute {
        val forceReads = forceVerdictReadsOption.value == true
        val forceMethods = forceVerdictMethodsOption.value == true
        if (!forceReads && !forceMethods) {
            logger.info("All options disabled.")
            return@execute
        }

        val invokePatched = if (forceReads) {
            patchMatchingMethods(
                isCandidate = { method ->
                    method.implementation?.instructions?.any(::isVerdictRead) == true
                },
            ) { instructions ->
                var count = 0
                for (index in instructions.indices) {
                    if (isVerdictRead(instructions[index]) &&
                        forceInvokeResult(instructions, index) { register -> "const/4 v$register, 0x1" }
                    ) {
                        count++
                    }
                }
                count
            }
        } else {
            0
        }

        val methodPatched = if (forceMethods) {
            patchMatchingMethods(
                isCandidate = ::isVerdictMethod,
            ) { instructions ->
                var count = 0
                // Descending so the inserted return is never revisited.
                for (index in instructions.indices.reversed()) {
                    val instruction = instructions.getOrNull(index) ?: continue
                    if (instruction.opcode == Opcode.RETURN && instruction is OneRegisterInstruction) {
                        replaceInstruction(index, "const/4 v${instruction.registerA}, 0x1")
                        addInstruction(index + 1, "return v${instruction.registerA}")
                        count++
                    }
                }
                count
            }
        } else {
            0
        }

        logger.info("Applied ${invokePatched + methodPatched} Play Integrity bypass replacements.")
    }
}

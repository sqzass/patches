package app.sqzass.patches.all.integrity

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.shared.forceInvokeResult
import app.sqzass.patches.shared.methodReferenceOf
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import java.util.logging.Logger

private val logger = Logger.getLogger("BypassPlayLicensingPatch")

// Matches com.android.vending.licensing and common forks/relocations.
private const val LICENSING_MARKER = "licens"

private val DENY_METHODS = setOf("dontAllow", "applicationError")

// PolicyResponse.LICENSED in the LVL library.
private const val LICENSED = 0

// ponytail: server-side License Verification Library responses cannot be
// forged; this only rewrites client-side handling. Obfuscated or relocated
// licensing code that no longer contains "licens" is missed; upgrade path is
// matching on method signatures instead of package names.
private fun isLicensingReference(className: String): Boolean =
    className.lowercase().contains(LICENSING_MARKER)

private fun isLicenseResponseQuery(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return isLicensingReference(reference.definingClass) &&
        reference.name == "processResponse" &&
        reference.returnType == "I"
}

private fun isDenyDispatch(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return isLicensingReference(reference.definingClass) &&
        reference.name in DENY_METHODS &&
        reference.returnType == "V"
}

@Suppress("unused")
val bypassPlayLicensingPatch = bytecodePatch(
    name = "Bypass Play licensing",
    description = "Makes licensing checks report success: forces licensing policy responses to licensed and suppresses denial callbacks. Server-side license verification cannot be bypassed this way.",
    default = false,
) {
    category("Integrity")

    val forceLicensedOption = booleanOption(
        key = "forceLicensedResponse",
        default = true,
        title = "Force Licensed Response",
        description = "Force licensing policy responses to report licensed.",
    )
    val suppressDenialsOption = booleanOption(
        key = "suppressDenialCallbacks",
        default = true,
        title = "Suppress Denial Callbacks",
        description = "Nop licensing denial and error callbacks.",
    )

    execute {
        val forceLicensed = forceLicensedOption.value == true
        val suppressDenials = suppressDenialsOption.value == true
        if (!forceLicensed && !suppressDenials) {
            logger.info("All options disabled.")
            return@execute
        }

        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.let { raw ->
                    val instructions = raw.toList()
                    (forceLicensed && instructions.any(::isLicenseResponseQuery)) ||
                        (suppressDenials && instructions.any(::isDenyDispatch))
                } == true
            },
        ) { instructions ->
            var count = 0
            if (forceLicensed) {
                for (index in instructions.indices) {
                    if (isLicenseResponseQuery(instructions[index]) &&
                        forceInvokeResult(instructions, index) { register -> "const/4 v$register, 0x$LICENSED" }
                    ) {
                        count++
                    }
                }
            }
            if (suppressDenials) {
                for (index in instructions.indices) {
                    if (isDenyDispatch(instructions[index])) {
                        replaceInstruction(index, "nop")
                        count++
                    }
                }
            }
            count
        }

        logger.info("Applied $patched Play licensing bypass replacements.")
    }
}

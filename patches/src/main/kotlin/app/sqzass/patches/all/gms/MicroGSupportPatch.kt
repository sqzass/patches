package app.sqzass.patches.all.gms

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.collectElements
import app.sqzass.patches.shared.forceInvokeResult
import app.sqzass.patches.shared.methodReferenceOf
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import java.util.logging.Logger

private val logger = Logger.getLogger("MicroGSupportPatch")

private val GMS_AVAILABILITY_CLASSES = setOf(
    "Lcom/google/android/gms/common/GoogleApiAvailability;",
    "Lcom/google/android/gms/common/GooglePlayServicesUtilLight;",
    "Lcom/google/android/gms/common/GooglePlayServicesUtil;",
)

private val ERROR_DIALOG_METHODS = setOf("showErrorDialogFragment", "showErrorNotification")

private const val GMS_VERSION_META = "com.google.android.gms.version"

// Unlike GMS check strip, package visibility queries are kept: MicroG and
// FakeStore live in com.google.android.gms / com.android.vending and must stay
// visible to the app for service binding and billing to work.
private fun isAvailabilityCheck(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass in GMS_AVAILABILITY_CLASSES &&
        reference.name == "isGooglePlayServicesAvailable" &&
        reference.returnType == "I"
}

private fun isUserResolvableError(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass in GMS_AVAILABILITY_CLASSES &&
        reference.name == "isUserResolvableError" &&
        reference.returnType == "Z"
}

private fun isErrorDialogCall(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass in GMS_AVAILABILITY_CLASSES &&
        reference.name in ERROR_DIALOG_METHODS &&
        reference.returnType == "V"
}

private val forceAvailabilityOption = booleanOption(
    key = "forceAvailabilitySuccess",
    default = true,
    title = "Force Availability Success",
    description = "Report isGooglePlayServicesAvailable as success (0) so MicroG can serve the APIs.",
)
private val suppressDialogsOption = booleanOption(
    key = "suppressErrorDialogs",
    default = true,
    title = "Suppress Error Dialogs",
    description = "Nop Google Play services error dialogs and notifications.",
)
private val removeVersionMetaOption = booleanOption(
    key = "removeGmsVersionMeta",
    default = true,
    title = "Remove GMS Version Meta",
    description = "Remove the com.google.android.gms.version meta-data entry used for client version checks.",
)

// Internal patch that is not shown in Morphe Manager or the CLI patch list.
@Suppress("unused")
private val microGSupportInternalPatch = bytecodePatch {
    execute {
        val forceAvailability = forceAvailabilityOption.value == true
        val suppressDialogs = suppressDialogsOption.value == true
        if (!forceAvailability && !suppressDialogs) {
            logger.info("All bytecode options disabled.")
            return@execute
        }

        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.let { raw ->
                    val instructions = raw.toList()
                    (forceAvailability && (
                        instructions.any(::isAvailabilityCheck) ||
                            instructions.any(::isUserResolvableError)
                        )) ||
                        (suppressDialogs && instructions.any(::isErrorDialogCall))
                } == true
            },
        ) { instructions ->
            var count = 0
            if (forceAvailability) {
                for (index in instructions.indices) {
                    val instruction = instructions[index]
                    if (isAvailabilityCheck(instruction) &&
                        forceInvokeResult(instructions, index) { register -> "const/16 v$register, 0x0" }
                    ) {
                        count++
                    } else if (isUserResolvableError(instruction) &&
                        forceInvokeResult(instructions, index) { register -> "const/4 v$register, 0x0" }
                    ) {
                        count++
                    }
                }
            }
            if (suppressDialogs) {
                for (index in instructions.indices) {
                    if (isErrorDialogCall(instructions[index])) {
                        replaceInstruction(index, "nop")
                        count++
                    }
                }
            }
            count
        }

        logger.info("Applied $patched MicroG support replacements.")
    }
}

@Suppress("unused")
val microGSupportPatch = resourcePatch(
    name = "MicroG support",
    description = "Adapts the app for MicroG: forces Play services availability checks to succeed, suppresses error dialogs, and drops the GMS client version check. MicroG is heavier than stripping GMS checks alone but more stable.",
    default = false,
) {
    category("GMS")

    forceAvailabilityOption()
    suppressDialogsOption()
    removeVersionMetaOption()

    dependsOn(microGSupportInternalPatch)

    execute {
        if (removeVersionMetaOption.value != true) {
            logger.info("GMS version meta removal is off.")
            return@execute
        }

        document("AndroidManifest.xml").use { document ->
            val metas = collectElements(document, listOf("meta-data")) {
                it.getAttribute("android:name") == GMS_VERSION_META
            }
            for (element in metas) {
                element.parentNode?.removeChild(element)
            }
            logger.info("Removed ${metas.size} GMS version meta-data entries.")
        }
    }
}
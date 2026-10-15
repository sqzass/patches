package app.sqzass.patches.all.gms

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.collectDescendants
import app.sqzass.patches.shared.collectElements
import app.sqzass.patches.shared.forceInvokeResult
import app.sqzass.patches.shared.methodReferenceOf
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import java.util.logging.Logger

private val logger = Logger.getLogger("GmsCheckStripPatch")

private val GMS_AVAILABILITY_CLASSES = setOf(
    "Lcom/google/android/gms/common/GoogleApiAvailability;",
    "Lcom/google/android/gms/common/GooglePlayServicesUtilLight;",
    "Lcom/google/android/gms/common/GooglePlayServicesUtil;",
)

private val GMS_VISIBILITY_PACKAGES = setOf(
    "com.google.android.gms",
    "com.android.vending",
)

private val ERROR_DIALOG_METHODS = setOf("showErrorDialogFragment", "showErrorNotification")

// ponytail: only direct GoogleApiAvailability calls are covered; apps that
// reflect on GMS or require live GMS services still need MicroG.
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
    description = "Report isGooglePlayServicesAvailable as success (0) and isUserResolvableError as false.",
)
private val suppressDialogsOption = booleanOption(
    key = "suppressErrorDialogs",
    default = true,
    title = "Suppress Error Dialogs",
    description = "Nop Google Play services error dialogs and notifications.",
)
private val stripQueriesOption = booleanOption(
    key = "stripGmsQueries",
    default = true,
    title = "Strip GMS Package Visibility",
    description = "Remove GMS and Play Store entries from manifest package visibility queries.",
)

// Internal patch that is not shown in Morphe Manager or the CLI patch list.
@Suppress("unused")
private val gmsCheckStripInternalPatch = bytecodePatch {
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

        logger.info("Applied $patched GMS check strip replacements.")
    }
}

@Suppress("unused")
val gmsCheckStripPatch = resourcePatch(
    name = "GMS check strip",
    description = "Makes the app run without Google Play services: forces availability checks to succeed, suppresses error dialogs, and strips GMS package visibility. A lighter alternative to MicroG, which is heavier but more stable.",
    default = false,
) {
    category("GMS")

    forceAvailabilityOption()
    suppressDialogsOption()
    stripQueriesOption()

    dependsOn(gmsCheckStripInternalPatch)

    execute {
        if (stripQueriesOption.value != true) {
            logger.info("GMS queries stripping is off.")
            return@execute
        }

        document("AndroidManifest.xml").use { document ->
            val packages = collectElements(document, listOf("queries")) { true }
                .flatMap { query -> collectDescendants(query, "package") }
                .filter { it.getAttribute("android:name") in GMS_VISIBILITY_PACKAGES }
            for (element in packages) {
                element.parentNode?.removeChild(element)
            }
            logger.info("Removed ${packages.size} GMS-related package visibility entries.")
        }
    }
}
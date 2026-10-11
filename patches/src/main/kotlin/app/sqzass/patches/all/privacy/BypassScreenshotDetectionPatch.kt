package app.sqzass.patches.all.privacy

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private val logger = Logger.getLogger("BypassScreenshotDetectionPatch")

// Matched by name because call-site method references may point at Activity subclasses
// that inherit these framework methods rather than at android.app.Activity itself.
// Both methods are void, so replacing the invoke with nop cannot leave a dangling
// move-result, and noping the pair keeps register/unregister balanced.
private val SCREEN_CAPTURE_CALLBACK_METHODS = setOf(
    "registerScreenCaptureCallback",
    "unregisterScreenCaptureCallback",
)

private fun isScreenCaptureCallbackCall(instruction: Instruction): Boolean {
    val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return reference.name in SCREEN_CAPTURE_CALLBACK_METHODS && reference.returnType == "V"
}

@Suppress("unused")
val bypassScreenshotDetectionPatch = bytecodePatch(
    name = "Bypass screenshot detection",
    description = "Disables screenshot detection callbacks so the app is not notified when a screenshot is taken.",
    default = false
) {
    category("Interface")

    execute {
        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.any(::isScreenCaptureCallbackCall) == true
            },
        ) { instructions ->
            var count = 0
            for (index in instructions.indices) {
                if (isScreenCaptureCallbackCall(instructions[index])) {
                    replaceInstruction(index, "nop")
                    count++
                }
            }
            count
        }

        logger.info("Disabled $patched screenshot detection callback registrations.")
    }
}
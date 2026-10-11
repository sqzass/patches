package app.sqzass.patches.all.privacy

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.shared.TrackerPrefixes
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private val logger = Logger.getLogger("DisableTrackerCallsPatch")

/**
 * Whether this is a void-returning call into a tracking SDK that is safe to nop.
 *
 * Constructors are skipped: noping `<init>` would leave the receiver uninitialized
 * and fail verification. Non-void calls are skipped as well: their result register
 * feeds application logic, so removing them would change control flow instead of
 * just suppressing the tracking side effect. And invoke-super is excluded so super
 * calls into framework base classes are never touched.
 */
private fun isNopableTrackerCall(instruction: Instruction): Boolean {
    if (instruction.opcode == Opcode.INVOKE_SUPER) return false
    val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return reference.returnType == "V" &&
        reference.name != "<init>" &&
        TrackerPrefixes.matches(reference.definingClass)
}

@Suppress("unused")
val disableTrackerCallsPatch = bytecodePatch(
    name = "Disable tracker SDK calls",
    description = "Replaces void-returning calls into known tracking SDKs with no-op instructions so analytics, attribution, and advertising code never runs at runtime.",
    default = false
) {
    category("Privacy")

    execute {
        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.any(::isNopableTrackerCall) == true
            },
        ) { instructions ->
            var count = 0
            for (index in instructions.indices) {
                if (isNopableTrackerCall(instructions[index])) {
                    replaceInstruction(index, "nop")
                    count++
                }
            }
            count
        }

        logger.info("Nopped $patched tracking SDK call sites.")
    }
}
package app.sqzass.patches.shared

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import java.util.logging.Logger

private val logger = Logger.getLogger("patchMatchingMethods")

/**
 * Run [patchMethod] over every method of every class where [isCandidate] holds.
 *
 * The scan runs against immutable classes first; only classes with a match are
 * copied to mutable form, so unrelated DEX entries are not marked as modified.
 *
 * @param isCandidate Whether a method contains instructions [patchMethod] would change.
 * @param patchMethod Receives the mutable method and its instructions, returns the
 *   number of instruction sites it changed.
 * @return The total number of changed instruction sites.
 */
fun BytecodePatchContext.patchMatchingMethods(
    isCandidate: (Method) -> Boolean,
    patchMethod: MutableMethod.(List<BuilderInstruction>) -> Int,
): Int {
    val candidateTypes = mutableSetOf<String>()
    classDefForEach { classDef ->
        if (classDef.methods.any(isCandidate)) candidateTypes += classDef.type
    }

    var patched = 0
    for (type in candidateTypes) {
        for (method in mutableClassDefBy(type).methods) {
            val implementation = method.implementation ?: continue
            // ponytail: the dexlib2 instruction list is a live view that shrinks when
            // deferred switch-payload/NOP fix-ups run; a per-method try/catch keeps one
            // stale index from aborting the whole patch session.
            try {
                patched += method.patchMethod(implementation.instructions)
            } catch (e: IndexOutOfBoundsException) {
                logger.warning("Skipping ${method.definingClass}->${method.name}: stale instruction index.")
            }
        }
    }
    return patched
}

private val MOVE_RESULT_OPCODES = setOf(
    Opcode.MOVE_RESULT,
    Opcode.MOVE_RESULT_WIDE,
    Opcode.MOVE_RESULT_OBJECT,
)

/**
 * Nop the invoke at [invokeIndex] and overwrite the move-result that immediately
 * follows it with [smaliForResult], which receives the destination register.
 *
 * Noping without a following move-result leaves no place to write the forced
 * value, so the invoke is skipped in that case (the result register would
 * otherwise hold a stale value).
 *
 * @return Whether a move-result followed the invoke and was replaced.
 */
fun MutableMethod.forceInvokeResult(
    instructions: List<BuilderInstruction>,
    invokeIndex: Int,
    smaliForResult: (Int) -> String,
): Boolean {
    // Live dexlib2 list resizes on deferred fix-ups, so a collected index may be
    // stale; bail out instead of throwing so one site never aborts the session.
    val invoke = instructions.getOrNull(invokeIndex) ?: return false
    if (!invoke.opcode.name.startsWith("INVOKE")) return false
    val next = instructions.getOrNull(invokeIndex + 1)
    if (next?.opcode !in MOVE_RESULT_OPCODES) return false
    replaceInstruction(invokeIndex, "nop")
    val register = (next as OneRegisterInstruction).registerA
    replaceInstruction(invokeIndex + 1, smaliForResult(register))
    return true
}
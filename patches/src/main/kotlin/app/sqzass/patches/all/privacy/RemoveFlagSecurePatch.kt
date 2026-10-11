package app.sqzass.patches.all.privacy

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private val logger = Logger.getLogger("RemoveFlagSecurePatch")

private const val FLAG_SECURE = 0x2000

private const val WINDOW_TYPE = "Landroid/view/Window;"
private const val LAYOUT_PARAMS_TYPE = "Landroid/view/WindowManager\$LayoutParams;"

private val WINDOW_FLAG_METHODS = setOf("addFlags", "setFlags")

private val LOAD_OPCODES = setOf(Opcode.CONST_16, Opcode.CONST)
private val MOVE_OPCODES = setOf(Opcode.MOVE, Opcode.MOVE_WIDE, Opcode.MOVE_OBJECT)
private val OR_OPCODES = setOf(
    Opcode.OR_INT,
    Opcode.OR_INT_2ADDR,
    Opcode.OR_INT_LIT8,
    Opcode.OR_INT_LIT16,
)

// ponytail: data flow is only tracked through moves and bitwise ORs within a short
// straight-line window. Flag settings assembled across branches or helper methods are
// missed; upgrade path is a proper per-method data-flow pass.
private const val LOOKAHEAD = 10

/** Registers read by [instruction], most-specific interface first (Three extends Two extends One). */
private fun registersOf(instruction: Instruction): Set<Int> = when (instruction) {
    is FiveRegisterInstruction -> listOf(
        instruction.registerC,
        instruction.registerD,
        instruction.registerE,
        instruction.registerF,
        instruction.registerG,
    ).take(instruction.registerCount).toSet()
    is RegisterRangeInstruction ->
        (instruction.startRegister until instruction.startRegister + instruction.registerCount).toSet()
    is ThreeRegisterInstruction -> setOf(instruction.registerA, instruction.registerB, instruction.registerC)
    is TwoRegisterInstruction -> setOf(instruction.registerA, instruction.registerB)
    is OneRegisterInstruction -> setOf(instruction.registerA)
    else -> emptySet()
}

private fun isFlagSecureLoad(instruction: Instruction): Boolean =
    instruction.opcode in LOAD_OPCODES &&
        instruction is NarrowLiteralInstruction &&
        instruction.narrowLiteral == FLAG_SECURE

/**
 * Whether the constant loaded at [index] into [register] flows into a
 * Window.addFlags/setFlags call or an assignment to LayoutParams.flags.
 */
private fun feedsWindowFlagSet(instructions: List<Instruction>, index: Int, register: Int): Boolean {
    val derived = mutableSetOf(register)
    val end = minOf(index + 1 + LOOKAHEAD, instructions.size)

    for (i in index + 1 until end) {
        val instruction = instructions[i]

        val reference = (instruction as? ReferenceInstruction)?.reference
        when (reference) {
            is MethodReference -> if (
                reference.definingClass == WINDOW_TYPE &&
                reference.name in WINDOW_FLAG_METHODS &&
                registersOf(instruction).any { it in derived }
            ) {
                return true
            }
            is FieldReference -> if (
                reference.definingClass == LAYOUT_PARAMS_TYPE &&
                reference.name == "flags" &&
                instruction is TwoRegisterInstruction &&
                instruction.registerA in derived
            ) {
                return true
            }
        }

        when {
            instruction.opcode in MOVE_OPCODES &&
                instruction is TwoRegisterInstruction &&
                instruction.registerB in derived -> derived += instruction.registerA

            instruction.opcode in OR_OPCODES -> {
                val sources = when (instruction) {
                    is ThreeRegisterInstruction -> setOf(instruction.registerB, instruction.registerC)
                    is TwoRegisterInstruction -> setOf(instruction.registerA, instruction.registerB)
                    else -> emptySet()
                }
                if (sources.any { it in derived } && instruction is OneRegisterInstruction) {
                    derived += instruction.registerA
                }
            }
        }
    }
    return false
}

/** Indices of constant loads that feed a secure-flag window operation. */
private fun findFlagSecureLoads(instructions: List<Instruction>): List<Int> =
    instructions.indices.filter { index ->
        val instruction = instructions[index]
        isFlagSecureLoad(instruction) &&
            feedsWindowFlagSet(instructions, index, (instruction as OneRegisterInstruction).registerA)
    }

@Suppress("unused")
val removeFlagSecurePatch = bytecodePatch(
    name = "Remove FLAG_SECURE",
    description = "Removes FLAG_SECURE from window flag operations so secure applications allow screenshots and screen recording.",
    default = false
) {
    category("Interface")

    execute {
        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.toList()?.let(::findFlagSecureLoads)?.isNotEmpty() == true
            },
        ) { instructions ->
            val loads = findFlagSecureLoads(instructions)
            for (index in loads) {
                val register = (instructions[index] as OneRegisterInstruction).registerA
                // Zeroing the constant turns addFlags(FLAG_SECURE) into a no-op and
                // setFlags(0, FLAG_SECURE) into a flag clear.
                replaceInstruction(index, "const/16 v$register, 0x0")
            }
            loads.size
        }

        logger.info("Cleared FLAG_SECURE at $patched call sites.")
    }
}

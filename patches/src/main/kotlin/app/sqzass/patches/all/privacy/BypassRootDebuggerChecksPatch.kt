package app.sqzass.patches.all.privacy

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.logging.Logger

private val logger = Logger.getLogger("BypassRootDebuggerChecksPatch")

private val DEBUGGER_CLASSES = setOf("Landroid/os/Debug;", "Ldalvik/system/VMDebug;")
private val DEBUGGER_BOOLEAN_METHODS = setOf("isDebuggerConnected", "waitingForDebugger")
private val DEBUGGER_VOID_METHODS = setOf("waitForDebugger")

private val FILE_PROBE_METHODS = setOf("exists", "canExecute", "canRead", "isDirectory")

private val ROOT_PATH_FRAGMENTS = setOf(
    "/system/xbin/su",
    "/system/bin/su",
    "/sbin/su",
    "magisk",
    "superuser",
    "busybox",
    "daemonsu",
    "supersu",
    "kernelsu",
    "test-keys",
    "which su",
)

private val MOVE_RESULT_OPCODES = setOf(
    Opcode.MOVE_RESULT,
    Opcode.MOVE_RESULT_WIDE,
    Opcode.MOVE_RESULT_OBJECT,
)

private fun isRootIndicator(instruction: Instruction): Boolean {
    val text = (instruction as? ReferenceInstruction)?.reference
        ?.let { it as? StringReference }?.string ?: return false
    return text == "su" || ROOT_PATH_FRAGMENTS.any { text.contains(it, ignoreCase = true) }
}

private fun planReplacements(
    instructions: List<Instruction>,
    bypassDebugger: Boolean,
    bypassRoot: Boolean,
): Map<Int, String> {
    val replacements = mutableMapOf<Int, String>()
    if (!bypassDebugger && !bypassRoot) return replacements

    val hasRootIndicators = bypassRoot && instructions.any(::isRootIndicator)

    fun forceFalse(invokeIndex: Int) {
        replacements[invokeIndex] = "nop"
        val next = instructions.getOrNull(invokeIndex + 1) ?: return
        if (next.opcode in MOVE_RESULT_OPCODES && next is OneRegisterInstruction) {
            replacements[invokeIndex + 1] = "const/16 v${next.registerA}, 0x0"
        }
    }

    for (index in instructions.indices) {
        val instruction = instructions[index]
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue

        if (bypassDebugger && reference.definingClass in DEBUGGER_CLASSES) {
            if (reference.name in DEBUGGER_BOOLEAN_METHODS && reference.returnType == "Z") {
                forceFalse(index)
            } else if (reference.name in DEBUGGER_VOID_METHODS && reference.returnType == "V") {
                replacements[index] = "nop"
            }
            continue
        }

        if (hasRootIndicators &&
            reference.definingClass == "Ljava/io/File;" &&
            reference.name in FILE_PROBE_METHODS &&
            reference.returnType == "Z"
        ) {
            forceFalse(index)
        }
    }
    return replacements
}

@Suppress("unused")
val bypassRootDebuggerChecksPatch = bytecodePatch(
    name = "Bypass root and debugger checks",
    description = "Reports the debugger as disconnected, hides root indicator files from existence checks. Works only on common detection methods.",
    default = false
) {
    category("Debugging")

    val bypassDebuggerChecksOption = booleanOption(
        key = "bypassDebuggerChecks",
        default = true,
        title = "Bypass Debugger Checks",
        description = "Force Debug.isDebuggerConnected, VMDebug.isDebuggerConnected, and waitingForDebugger to report false.",
    )

    val bypassRootFileChecksOption = booleanOption(
        key = "bypassRootFileChecks",
        default = true,
        title = "Bypass Root File Checks",
        description = "Make File.exists and related probes report false inside methods that reference su, magisk, or busybox paths.",
    )

    execute {
        val bypassDebugger = bypassDebuggerChecksOption.value == true
        val bypassRoot = bypassRootFileChecksOption.value == true

        if (!bypassDebugger && !bypassRoot) {
            logger.info("Both options disabled.")
            return@execute
        }

        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.toList()
                    ?.let { planReplacements(it, bypassDebugger, bypassRoot).isNotEmpty() } == true
            },
        ) { instructions ->
            val plan = planReplacements(instructions, bypassDebugger, bypassRoot)
            plan.forEach { (index, smali) -> replaceInstruction(index, smali) }
            plan.size
        }

        logger.info("Applied $patched anti-detection bytecode replacements.")
    }
}

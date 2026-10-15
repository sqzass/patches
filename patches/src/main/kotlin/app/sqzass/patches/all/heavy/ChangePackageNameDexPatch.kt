package app.sqzass.patches.all.heavy

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.shared.constStringLoad
import app.sqzass.patches.shared.forceInvokeResult
import app.sqzass.patches.shared.methodReferenceOf
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import java.util.logging.Logger

private val logger = Logger.getLogger("ChangePackageNameDexPatch")

private val rewriteDexStringsOption = booleanOption(
    key = "rewriteDexStrings",
    default = true,
    title = "Rewrite Dex Strings",
    description = "Rewrite matching package string constants in dex code.",
)
private val hookPackageNameOption = booleanOption(
    key = "hookGetPackageName",
    default = true,
    title = "Hook getPackageName",
    description = "Force Context.getPackageName to report the original package.",
)
private val oldPackageOption = app.morphe.patcher.patch.stringOption(
    key = "originalPackageName",
    default = "",
    title = "Original Package Name",
    description = "Package name to replace, as read from the manifest before renaming.",
    validator = { true },
)
private val dexNewPackageOption = app.morphe.patcher.patch.stringOption(
    key = "dexNewPackageName",
    default = "com.example.renamed",
    title = "New Package Name",
    description = "Replacement package name written into dex strings.",
    validator = { true },
)

private fun isPackageNameGetter(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass.startsWith("Landroid/content/Context") &&
        reference.name == "getPackageName" &&
        reference.returnType == "Ljava/lang/String;"
}

// ponytail: the original package is supplied via option because resource and
// bytecode patches cannot share state; the manager UI fills it from the
// manifest package before renaming.
@Suppress("unused")
val changePackageNameDexPatch = bytecodePatch(
    name = "Change package name (dex)",
    description = "Rewrites matching package string constants in dex code and can report the original package to the app.",
    default = false,
) {
    category("Heavy")

    oldPackageOption()
    dexNewPackageOption()
    rewriteDexStringsOption()
    hookPackageNameOption()

    execute {
        val oldPackage = oldPackageOption.value?.takeIf { it.isNotBlank() } ?: return@execute
        val newPackage = dexNewPackageOption.value?.takeIf { it.isNotBlank() } ?: return@execute
        if (rewriteDexStringsOption.value != true && hookPackageNameOption.value != true) {
            logger.info("Dex rewrite and getPackageName hook are off.")
            return@execute
        }

        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.let { raw ->
                    val instructions = raw.toList()
                    (rewriteDexStringsOption.value == true &&
                        instructions.any { constStringLoad(it)?.contains(oldPackage) == true }) ||
                        (hookPackageNameOption.value == true && instructions.any(::isPackageNameGetter))
                } == true
            },
        ) { instructions ->
            var count = 0
            if (rewriteDexStringsOption.value == true) {
                for (index in instructions.indices) {
                    if (instructions[index] !is ReferenceInstruction) continue
                    val payload = constStringLoad(instructions[index]) ?: continue
                    if (oldPackage !in payload) continue
                    val register = (instructions[index] as? OneRegisterInstruction)?.registerA ?: continue
                    replaceInstruction(index, "const-string v$register, \"${payload.replace(oldPackage, newPackage)}\"")
                    count++
                }
            }
            if (hookPackageNameOption.value == true) {
                for (index in instructions.indices) {
                    if (isPackageNameGetter(instructions[index]) &&
                        forceInvokeResult(instructions, index) { register -> "const-string v$register, \"$oldPackage\"" }
                    ) {
                        count++
                    }
                }
            }
            count
        }

        logger.info("Applied $patched package rename replacements ($oldPackage -> $newPackage).")
    }
}

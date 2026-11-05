package app.sqzass.patches.all.spoofing

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.shared.constStringLoad
import app.sqzass.patches.shared.findConstArgInvokes
import app.sqzass.patches.shared.forceInvokeResult
import app.sqzass.patches.shared.methodReferenceOf
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import java.util.logging.Logger

private val logger = Logger.getLogger("MockLocationFlagStripPatch")

private val MOCK_LOCATION_KEYS = setOf("mock_location", "android:mock_location")
private val MOCK_FLAG_METHODS = setOf("isFromMockProvider", "isMock")
private val SETTINGS_READ_METHODS = setOf("getInt", "getString", "getLong")

// ponytail: AppOps checks (OP_MOCK_LOCATION) and reflection-based reads are not
// covered; upgrade path is tracking IAppOpsService calls and Method.invoke targets.
private fun isMockFlagRead(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass == "Landroid/location/Location;" &&
        reference.name in MOCK_FLAG_METHODS &&
        reference.returnType == "Z"
}

private fun isSettingsRead(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass.startsWith("Landroid/provider/Settings$") &&
        reference.name in SETTINGS_READ_METHODS
}

private fun isMockKeyLoad(instruction: Instruction): Boolean =
    MOCK_LOCATION_KEYS.contains(constStringLoad(instruction))

@Suppress("unused")
val mockLocationFlagStripPatch = bytecodePatch(
    name = "Strip mock location flags",
    description = "Reports Location.isMock and isFromMockProvider as false and forces mock location settings reads to report disabled.",
    default = false,
) {
    category("Spoofing")

    val bypassLocationApiOption = booleanOption(
        key = "bypassLocationApi",
        default = true,
        title = "Bypass Location Mock APIs",
        description = "Force Location.isMock and Location.isFromMockProvider to report false.",
    )
    val bypassSettingsOption = booleanOption(
        key = "bypassMockSettings",
        default = true,
        title = "Bypass Mock Location Settings",
        description = "Force Settings reads of the mock_location key to report disabled.",
    )

    execute {
        val bypassApi = bypassLocationApiOption.value == true
        val bypassSettings = bypassSettingsOption.value == true
        if (!bypassApi && !bypassSettings) {
            logger.info("All options disabled.")
            return@execute
        }

        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.let { raw ->
                    val instructions = raw.toList()
                    (bypassApi && instructions.any(::isMockFlagRead)) ||
                        (bypassSettings && findConstArgInvokes(
                            instructions,
                            ::isMockKeyLoad,
                            ::isSettingsRead,
                        ).isNotEmpty())
                } == true
            },
        ) { instructions ->
            var count = 0
            if (bypassApi) {
                for (index in instructions.indices) {
                    if (isMockFlagRead(instructions[index]) &&
                        forceInvokeResult(instructions, index) { register -> "const/4 v$register, 0x0" }
                    ) {
                        count++
                    }
                }
            }
            if (bypassSettings) {
                val forced = findConstArgInvokes(instructions, ::isMockKeyLoad, ::isSettingsRead)
                for (index in forced.sortedDescending()) {
                    val returnType = methodReferenceOf(instructions.getOrNull(index) ?: continue)?.returnType
                    if (forceInvokeResult(instructions, index) { register ->
                            when (returnType) {
                                "J" -> "const-wide v$register, 0x0"
                                "Ljava/lang/String;" -> "const-string v$register, \"0\""
                                else -> "const/4 v$register, 0x0"
                            }
                        }
                    ) {
                        count++
                    }
                }
            }
            count
        }

        logger.info("Applied $patched mock location flag replacements.")
    }
}
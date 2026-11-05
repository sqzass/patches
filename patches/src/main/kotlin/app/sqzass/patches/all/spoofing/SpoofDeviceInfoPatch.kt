package app.sqzass.patches.all.spoofing

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.sqzass.patches.shared.DevicePresets
import app.sqzass.patches.shared.DevicePreset
import app.sqzass.patches.shared.SimPreset
import app.sqzass.patches.shared.constStringLoad
import app.sqzass.patches.shared.findConstArgInvokes
import app.sqzass.patches.shared.forceInvokeResult
import app.sqzass.patches.shared.methodReferenceOf
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import java.util.logging.Logger

private val logger = Logger.getLogger("SpoofDeviceInfoPatch")

private const val TELEPHONY_MANAGER = "Landroid/telephony/TelephonyManager;"
private const val SYSTEM_PROPERTIES = "Landroid/os/SystemProperties;"

// Build fields rewritten at their sget read sites.
private val BUILD_FIELDS = setOf(
    "MANUFACTURER", "BRAND", "MODEL", "DEVICE", "PRODUCT", "HARDWARE", "DISPLAY", "FINGERPRINT",
)

private val DEVICE_ID_METHODS = setOf("getDeviceId", "getImei", "getMeid")

/** android.os.Build field name → preset value. */
private fun buildValues(preset: DevicePreset): Map<String, String> = mapOf(
    "MANUFACTURER" to preset.manufacturer,
    "BRAND" to preset.brand,
    "MODEL" to preset.model,
    "DEVICE" to preset.device,
    "PRODUCT" to preset.product,
    "HARDWARE" to preset.hardware,
    "DISPLAY" to preset.display,
    "FINGERPRINT" to preset.fingerprint,
)

/** System property key → preset value, for direct SystemProperties.get reads. */
private fun systemProperties(preset: DevicePreset): Map<String, String> = mapOf(
    "ro.product.manufacturer" to preset.manufacturer,
    "ro.product.brand" to preset.brand,
    "ro.product.model" to preset.model,
    "ro.product.device" to preset.device,
    "ro.product.name" to preset.product,
    "ro.hardware" to preset.hardware,
)

/** Telephony getter name → preset value (operator info plus SIM identity). */
private fun telephonyResults(sim: SimPreset): Map<String, String> = mapOf(
    "getSimOperator" to sim.operatorCode,
    "getNetworkOperator" to sim.operatorCode,
    "getSimOperatorName" to sim.carrier,
    "getNetworkOperatorName" to sim.carrier,
    "getSimCountryIso" to sim.countryIso,
    "getNetworkCountryIso" to sim.countryIso,
    "getSimSerialNumber" to sim.iccid,
    "getSubscriberId" to sim.imsi,
)

// ponytail: reflection-based reads (Build via getprop, telephony via
// Method.invoke), eSIM/profile APIs, and Wi-Fi MAC identity are not covered;
// upgrade path is hooking Method.invoke call sites.
private fun isBuildFieldLoad(instruction: Instruction): Boolean {
    val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference ?: return false
    return reference.definingClass == "Landroid/os/Build;" && reference.name in BUILD_FIELDS
}

private fun isSystemPropertiesGet(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass == SYSTEM_PROPERTIES &&
        reference.name == "get" &&
        reference.returnType == "Ljava/lang/String;"
}

private fun isForcedTelephonyGetter(instruction: Instruction, keys: Set<String>): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass == TELEPHONY_MANAGER &&
        reference.returnType == "Ljava/lang/String;" &&
        reference.name in keys
}

@Suppress("unused")
val spoofDeviceInfoPatch = bytecodePatch(
    name = "Spoof device information",
    description = "Presents a different device entirely: rewrites Build identity field reads, forces matching system property reads, and overrides telephony operator and SIM identity getters using device and SIM presets.",
    default = false,
) {
    category("Spoofing")

    val devicePresetOption = stringOption(
        key = "devicePreset",
        default = "google-pixel-8",
        values = DevicePresets.DEVICE_LABELS,
        title = "Device Preset",
        description = "Device build identity to present, including brand, model, and fingerprint.",
    )
    val simPresetOption = stringOption(
        key = "simPreset",
        default = "tmobile-us",
        values = DevicePresets.SIM_LABELS,
        title = "SIM Preset",
        description = "Carrier identity to present, including operator code, country, ICCID, and IMSI.",
    )
    val spoofBuildOption = booleanOption(
        key = "spoofBuildIdentity",
        default = true,
        title = "Spoof Build Identity",
        description = "Rewrite android.os.Build field reads and matching ro.* system property reads to the preset.",
    )
    val spoofSimOption = booleanOption(
        key = "spoofSimIdentity",
        default = true,
        title = "Spoof SIM Identity",
        description = "Force TelephonyManager operator, country, ICCID, and IMSI getters to the SIM preset.",
    )
    val spoofIdsOption = booleanOption(
        key = "spoofDeviceIds",
        default = false,
        title = "Spoof Device IDs",
        description = "Force getDeviceId, getImei, and getMeid to the preset IMEI.",
    )

    execute {
        val preset = DevicePresets.DEVICES[devicePresetOption.value]
            ?: DevicePresets.DEVICES.values.first()
                .also { logger.warning("Unknown device preset, falling back to the first entry.") }
        val sim = DevicePresets.SIMS[simPresetOption.value]
            ?: DevicePresets.SIMS.values.first()
                .also { logger.warning("Unknown SIM preset, falling back to the first entry.") }

        val spoofBuild = spoofBuildOption.value == true
        val spoofSim = spoofSimOption.value == true
        val spoofIds = spoofIdsOption.value == true
        if (!spoofBuild && !spoofSim && !spoofIds) {
            logger.info("All options disabled.")
            return@execute
        }

        val buildValues = buildValues(preset)
        val props = systemProperties(preset)
        val telephony = telephonyResults(sim)

        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.let { raw ->
                    val instructions = raw.toList()
                    (spoofBuild && (
                        instructions.any(::isBuildFieldLoad) ||
                            props.keys.any { key ->
                                findConstArgInvokes(
                                    instructions,
                                    { constStringLoad(it) == key },
                                    ::isSystemPropertiesGet,
                                ).isNotEmpty()
                            }
                        )) ||
                        (spoofSim && instructions.any { isForcedTelephonyGetter(it, telephony.keys) }) ||
                        (spoofIds && instructions.any { isForcedTelephonyGetter(it, DEVICE_ID_METHODS) })
                } == true
            },
        ) { instructions ->
            var count = 0

            if (spoofBuild) {
                // Iterate descending over a snapshot: replaceInstruction keeps the
                // size but pending dexlib2 fix-ups can still shift the live view.
                for (index in instructions.indices.reversed()) {
                    val instruction = instructions.getOrNull(index) ?: continue
                    if (!isBuildFieldLoad(instruction) || instruction !is OneRegisterInstruction) continue
                    val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference ?: continue
                    val value = buildValues[reference.name] ?: continue
                    replaceInstruction(index, "const-string v${instruction.registerA}, \"$value\"")
                    count++
                }
                for ((key, value) in props) {
                    for (index in findConstArgInvokes(
                        instructions,
                        { constStringLoad(it) == key },
                        ::isSystemPropertiesGet,
                    ).sortedDescending()) {
                        if (forceInvokeResult(instructions, index) { register -> "const-string v$register, \"$value\"" }) {
                            count++
                        }
                    }
                }
            }

            if (spoofSim) {
                for (index in instructions.indices.reversed()) {
                    val target = instructions.getOrNull(index) ?: continue
                    val value = methodReferenceOf(target)?.name?.let { telephony[it] }
                    if (value != null &&
                        isForcedTelephonyGetter(target, telephony.keys) &&
                        forceInvokeResult(instructions, index) { register -> "const-string v$register, \"$value\"" }
                    ) {
                        count++
                    }
                }
            }

            if (spoofIds) {
                for (index in instructions.indices.reversed()) {
                    val target = instructions.getOrNull(index) ?: continue
                    if (isForcedTelephonyGetter(target, DEVICE_ID_METHODS) &&
                        forceInvokeResult(instructions, index) { register -> "const-string v$register, \"${preset.imei}\"" }
                    ) {
                        count++
                    }
                }
            }

            count
        }

        logger.info("Applied $patched device identity spoof replacements for ${preset.model} / ${sim.carrier}.")
    }
}
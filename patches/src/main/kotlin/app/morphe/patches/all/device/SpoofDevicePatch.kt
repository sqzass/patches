package app.morphe.patches.all.spoof.device

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.findMutableMethodOf
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private class Preset(
    val label: String, val manufacturer: String, val brand: String, val model: String,
    val device: String, val product: String, val board: String, val hardware: String,
    val release: String, val sdk: Int, val patch: String, val id: String,
    val incremental: String, val bootloader: String, val radio: String,
) {
    val fingerprint get() = "$brand/$product/$device:$release/$id/$incremental:user/release-keys"
    val display get() = id
}

private val PRESETS = listOf(
    Preset("Pixel 8 Pro", "Google", "google", "Pixel 8 Pro", "husky", "husky", "husky", "tensor", "14", 34, "2024-05-05", "AP1A.240505.004", "11677807", "husky-1.0-11677807", "unknown"),
    Preset("Pixel 7", "Google", "google", "Pixel 7", "panther", "panther", "panther", "tensor", "14", 34, "2024-05-05", "AP1A.240505.004", "11677807", "panther-1.0-11677807", "unknown"),
    Preset("Galaxy S24 Ultra", "samsung", "samsung", "SM-S928B", "e3q", "e3qxxx", "e3q", "qcom", "14", 34, "2024-01-01", "UP1A.231005.007", "S928BXXU1AXCA", "S928BXXU1AXCA", "S928BXXU1AXCA"),
    Preset("Galaxy S23", "samsung", "samsung", "SM-S911B", "dm3q", "dm3qxxx", "dm3q", "qcom", "14", 34, "2024-01-01", "UP1A.231005.007", "S911BXXU3CWK1", "S911BXXU3CWK1", "S911BXXU3CWK1"),
    Preset("OnePlus 12", "OnePlus", "OnePlus", "CPH2581", "OP594DL1", "CPH2581", "aston", "qcom", "14", 34, "2024-01-05", "UKQ1.230924.001", "R.20240105", "CPH2581_11.A.01", "unknown"),
    Preset("Xiaomi 14", "Xiaomi", "Xiaomi", "23127PN0CG", "houji", "houji", "houji", "qcom", "14", 34, "2024-01-01", "UKQ1.230917.001", "V816.0.4.0.UNCCNXM", "unknown", "unknown"),
    Preset("Nothing Phone (2)", "Nothing", "Nothing", "A065", "Pong", "Pong", "Pong", "qcom", "14", 34, "2024-01-05", "UP1A.231005.007", "20240105", "unknown", "unknown"),
    Preset("Motorola Edge 40", "motorola", "motorola", "motorola edge 40", "lyriq", "lyriq", "lyriq", "mt6895", "14", 34, "2024-01-01", "UP1A.231005.007", "12345", "unknown", "unknown"),
)

@Suppress("unused")
val spoofDevicePatch = bytecodePatch(
    name = "Spoof device information",
    description = "Replaces android.os.Build device identity fields with a chosen device preset.",
    default = false,
) {
    val deviceOption = stringOption(
        key = "device", default = PRESETS[0].label,
        values = PRESETS.associate { it.label to it.label },
        title = "Device preset", description = "The device to impersonate.", required = true,
    )
    val spoofVersion by booleanOption(
        key = "spoofVersion", default = false,
        title = "Also spoof OS version",
        description = "Also spoof Build.VERSION (RELEASE, SDK_INT, ...). Can break apps; off by default.",
    )

    execute {
        val p = PRESETS.first { it.label == deviceOption.value }
        val buildStrings = mapOf(
            "MANUFACTURER" to p.manufacturer, "BRAND" to p.brand, "MODEL" to p.model,
            "DEVICE" to p.device, "PRODUCT" to p.product, "BOARD" to p.board, "HARDWARE" to p.hardware,
            "FINGERPRINT" to p.fingerprint, "DISPLAY" to p.display, "ID" to p.id,
            "BOOTLOADER" to p.bootloader, "RADIO" to p.radio,
            "TAGS" to "release-keys", "TYPE" to "user", "USER" to "android-build", "HOST" to "abfarm",
        )
        val versionStrings = mapOf(
            "RELEASE" to p.release, "SECURITY_PATCH" to p.patch,
            "INCREMENTAL" to p.incremental, "CODENAME" to "REL",
        )
        val ver = spoofVersion == true

        fun Instruction.isTarget(): Boolean {
            val f = getReference<FieldReference>()
            if (f != null) {
                if (opcode == Opcode.SGET_OBJECT && f.definingClass == "Landroid/os/Build;" && f.name in buildStrings) return true
                if (ver && f.definingClass == "Landroid/os/Build\$VERSION;") {
                    if (opcode == Opcode.SGET && f.name == "SDK_INT") return true
                    if (opcode == Opcode.SGET_OBJECT && f.name in versionStrings) return true
                }
                return false
            }
            val m = getReference<MethodReference>() ?: return false
            return m.definingClass == "Landroid/os/Build;" && m.name == "getRadioVersion"
        }

        classDefForEach { classDef ->
            val mc by lazy { mutableClassDefBy(classDef) }
            classDef.methods.forEach { method ->
                val impl = method.implementation ?: return@forEach
                if (impl.instructions.none { it.isTarget() }) return@forEach
                val mm = mc.findMutableMethodOf(method)
                val idxs = mm.instructions.withIndex().filter { it.value.isTarget() }.map { it.index }
                for (i in idxs) {
                    val ins = mm.getInstruction(i)
                    val f = ins.getReference<FieldReference>()
                    if (f != null) {
                        val reg = (ins as OneRegisterInstruction).registerA
                        when {
                            f.definingClass == "Landroid/os/Build;" && f.name in buildStrings ->
                                mm.replaceInstruction(i, "const-string v$reg, \"${buildStrings.getValue(f.name)}\"")
                            ver && f.definingClass == "Landroid/os/Build\$VERSION;" && f.name == "SDK_INT" ->
                                mm.replaceInstruction(i, "const v$reg, ${p.sdk}")
                            ver && f.definingClass == "Landroid/os/Build\$VERSION;" && f.name in versionStrings ->
                                mm.replaceInstruction(i, "const-string v$reg, \"${versionStrings.getValue(f.name)}\"")
                        }
                    } else {
                        val ni = i + 1
                        if (ni < mm.instructions.size && mm.getInstruction(ni).opcode == Opcode.MOVE_RESULT_OBJECT) {
                            val reg = (mm.getInstruction(ni) as OneRegisterInstruction).registerA
                            mm.replaceInstruction(ni, "const-string v$reg, \"${p.radio}\"")
                        }
                    }
                }
            }
        }
    }
}
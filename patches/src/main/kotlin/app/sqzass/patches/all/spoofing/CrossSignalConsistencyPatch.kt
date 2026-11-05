package app.sqzass.patches.all.spoofing

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.shared.forceInvokeResult
import app.sqzass.patches.shared.methodReferenceOf
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import java.util.logging.Logger

private val logger = Logger.getLogger("CrossSignalConsistencyPatch")

// ponytail: these reads return null (empty), so cross-checks find no
// contradicting fingerprint; fabricating BSSIDs/cell IDs consistent with the
// spoofed coordinates would require a geolocation database — upgrade path is
// an extension that builds synthetic scan results from the patched position.
private fun isWifiScanResults(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass == "Landroid/net/wifi/WifiManager;" &&
        reference.name == "getScanResults" &&
        reference.returnType == "Ljava/util/List;"
}

private fun isCellInfoRead(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass == "Landroid/telephony/TelephonyManager;" &&
        reference.name in setOf("getAllCellInfo", "getCellLocation") &&
        reference.returnType.startsWith("L")
}

@Suppress("unused")
val crossSignalConsistencyPatch = bytecodePatch(
    name = "Cross-signal location consistency",
    description = "Returns no Wi-Fi scan or cell tower data so location cross-checks against Wi-Fi fingerprints and cell IDs cannot contradict the spoofed position.",
    default = false,
) {
    category("Spoofing")

    val suppressWifiScansOption = booleanOption(
        key = "suppressWifiScans",
        default = true,
        title = "Suppress Wi-Fi Scans",
        description = "Force WifiManager.getScanResults to return null.",
    )
    val suppressCellInfoOption = booleanOption(
        key = "suppressCellInfo",
        default = true,
        title = "Suppress Cell Info",
        description = "Force TelephonyManager.getAllCellInfo and getCellLocation to return null.",
    )

    execute {
        val suppressWifi = suppressWifiScansOption.value == true
        val suppressCells = suppressCellInfoOption.value == true
        if (!suppressWifi && !suppressCells) {
            logger.info("All options disabled.")
            return@execute
        }

        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.let { raw ->
                    val instructions = raw.toList()
                    (suppressWifi && instructions.any(::isWifiScanResults)) ||
                        (suppressCells && instructions.any(::isCellInfoRead))
                } == true
            },
        ) { instructions ->
            var count = 0
            for (index in instructions.indices) {
                val instruction = instructions[index]
                val matches = (suppressWifi && isWifiScanResults(instruction)) ||
                    (suppressCells && isCellInfoRead(instruction))
                if (matches &&
                    forceInvokeResult(instructions, index) { register -> "const/4 v$register, 0x0" }
                ) {
                    count++
                }
            }
            count
        }

        logger.info("Applied $patched cross-signal suppression replacements.")
    }
}
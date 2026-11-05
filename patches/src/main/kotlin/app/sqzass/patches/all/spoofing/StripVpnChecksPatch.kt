package app.sqzass.patches.all.spoofing

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.shared.constStringLoad
import app.sqzass.patches.shared.findConstArgInvokes
import app.sqzass.patches.shared.forceInvokeResult
import app.sqzass.patches.shared.methodReferenceOf
import app.sqzass.patches.shared.narrowConstLoad
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import java.util.logging.Logger

private val logger = Logger.getLogger("StripVpnChecksPatch")

private const val TRANSPORT_VPN = 4

private val STRING_TEST_METHODS = setOf("equals", "equalsIgnoreCase", "contains")

private fun isVpnServicePrepare(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass == "Landroid/net/VpnService;" &&
        reference.name == "prepare" &&
        reference.returnType == "Landroid/content/Intent;"
}

private fun isHasTransport(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass == "Landroid/net/NetworkCapabilities;" &&
        reference.name == "hasTransport" &&
        reference.returnType == "Z"
}

private fun isVpnStringTest(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass == "Ljava/lang/String;" &&
        reference.name in STRING_TEST_METHODS &&
        reference.returnType == "Z"
}

private fun isVpnConstLoad(instruction: Instruction): Boolean =
    constStringLoad(instruction).equals("vpn", ignoreCase = true)

@Suppress("unused")
val stripVpnChecksPatch = bytecodePatch(
    name = "Strip VPN checks",
    description = "Defeats VPN detection: reports VpnService.prepare as no consent required, the VPN transport flag as unset, and VPN string tests as false.",
    default = false,
) {
    category("Spoofing")

    val bypassPrepareOption = booleanOption(
        key = "bypassVpnPrepare",
        default = true,
        title = "Bypass VpnService.prepare",
        description = "Force VpnService.prepare to return null so no VPN consent prompt is issued.",
    )
    val bypassTransportOption = booleanOption(
        key = "bypassVpnTransport",
        default = true,
        title = "Bypass VPN Transport Flag",
        description = "Force NetworkCapabilities.hasTransport(TRANSPORT_VPN) to report false.",
    )
    val bypassStringOption = booleanOption(
        key = "bypassVpnStringChecks",
        default = true,
        title = "Bypass VPN String Checks",
        description = "Force equals, equalsIgnoreCase, and contains tests against \"vpn\" string constants to report false.",
    )

    execute {
        val bypassPrepare = bypassPrepareOption.value == true
        val bypassTransport = bypassTransportOption.value == true
        val bypassString = bypassStringOption.value == true
        if (!bypassPrepare && !bypassTransport && !bypassString) {
            logger.info("All options disabled.")
            return@execute
        }

        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.let { raw ->
                    val instructions = raw.toList()
                    (bypassPrepare && instructions.any(::isVpnServicePrepare)) ||
                        (bypassTransport && findConstArgInvokes(
                            instructions,
                            { narrowConstLoad(it) == TRANSPORT_VPN },
                            ::isHasTransport,
                        ).isNotEmpty()) ||
                        (bypassString && findConstArgInvokes(
                            instructions,
                            ::isVpnConstLoad,
                            ::isVpnStringTest,
                        ).isNotEmpty())
                } == true
            },
        ) { instructions ->
            var count = 0
            if (bypassPrepare) {
                for (index in instructions.indices) {
                    if (isVpnServicePrepare(instructions[index]) &&
                        forceInvokeResult(instructions, index) { register -> "const/4 v$register, 0x0" }
                    ) {
                        count++
                    }
                }
            }
            if (bypassTransport) {
                val forced = findConstArgInvokes(
                    instructions,
                    { narrowConstLoad(it) == TRANSPORT_VPN },
                    ::isHasTransport,
                )
                for (index in forced) {
                    if (forceInvokeResult(instructions, index) { register -> "const/4 v$register, 0x0" }) count++
                }
            }
            if (bypassString) {
                val forced = findConstArgInvokes(instructions, ::isVpnConstLoad, ::isVpnStringTest)
                for (index in forced) {
                    if (forceInvokeResult(instructions, index) { register -> "const/4 v$register, 0x0" }) count++
                }
            }
            count
        }

        logger.info("Applied $patched anti-VPN bytecode replacements.")
    }
}
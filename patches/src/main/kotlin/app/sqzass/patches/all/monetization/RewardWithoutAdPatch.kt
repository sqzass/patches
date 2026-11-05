package app.sqzass.patches.all.monetization

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.shared.forceInvokeResult
import app.sqzass.patches.shared.methodReferenceOf
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import java.util.logging.Logger

private val logger = Logger.getLogger("RewardWithoutAdPatch")

private val AD_CLASS_MARKERS = setOf(
    "/ads/", "rewardedad", "unityads", "applovin", "ironsource",
    "vungle", "chartboost", "adcolony", "tapjoy", "fyber", "inmobi", "mintegral",
)

private val READINESS_METHODS = setOf(
    "isLoaded", "isReady", "isAdLoaded", "hasRewardedAd", "isRewardedAdReady", "canShow",
)

private val LOAD_FAILURE_METHODS = setOf(
    "onAdFailedToLoad", "onRewardedAdFailedToLoad", "onLoadFailed", "onAdLoadFailed",
)

// ponytail: granting the reward without showing an ad requires invoking the
// app's reward listener, which needs a fabricated call through an .mpe
// extension. This patch only forces readiness and suppresses load failures;
// upgrade path is extendWith-based listener invocation.
private fun isAdClass(type: String): Boolean {
    val lower = type.lowercase()
    return AD_CLASS_MARKERS.any { it in lower }
}

private fun isReadinessRead(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return isAdClass(reference.definingClass) &&
        reference.name in READINESS_METHODS &&
        reference.returnType == "Z"
}

private fun isLoadFailure(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return isAdClass(reference.definingClass) &&
        reference.name in LOAD_FAILURE_METHODS &&
        reference.returnType == "V"
}

@Suppress("unused")
val rewardWithoutAdPatch = bytecodePatch(
    name = "Ads-free rewards",
    description = "Experimental: reports rewarded ads as loaded and suppresses ad load failure callbacks so reward flows proceed. Granting rewards with no ad shown needs an extension and is not covered.",
    default = false,
) {
    category("Monetization")

    val forceReadyOption = booleanOption(
        key = "forceAdReady",
        default = true,
        title = "Force Ad Ready",
        description = "Force rewarded ad readiness checks to report loaded.",
    )
    val suppressFailuresOption = booleanOption(
        key = "suppressLoadFailures",
        default = true,
        title = "Suppress Load Failures",
        description = "Nop rewarded ad load failure callbacks.",
    )

    execute {
        val forceReady = forceReadyOption.value == true
        val suppressFailures = suppressFailuresOption.value == true
        if (!forceReady && !suppressFailures) {
            logger.info("All options disabled.")
            return@execute
        }

        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.let { raw ->
                    val instructions = raw.toList()
                    (forceReady && instructions.any(::isReadinessRead)) ||
                        (suppressFailures && instructions.any(::isLoadFailure))
                } == true
            },
        ) { instructions ->
            var count = 0
            for (index in instructions.indices) {
                val instruction = instructions[index]
                if (forceReady && isReadinessRead(instruction) &&
                    forceInvokeResult(instructions, index) { register -> "const/4 v$register, 0x1" }
                ) {
                    count++
                } else if (suppressFailures && isLoadFailure(instruction)) {
                    replaceInstruction(index, "nop")
                    count++
                }
            }
            count
        }

        logger.info("Applied $patched ads-free reward replacements.")
    }
}

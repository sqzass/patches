package app.sqzass.patches.all.monetization

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.sqzass.patches.shared.constStringLoad
import app.sqzass.patches.shared.findConstArgInvokes
import app.sqzass.patches.shared.forceInvokeResult
import app.sqzass.patches.shared.methodReferenceOf
import app.sqzass.patches.shared.patchMatchingMethods
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import java.util.logging.Logger

private val logger = Logger.getLogger("UnlockPremiumPatch")

private val PREMIUM_GETTERS = setOf(
    "isPremium", "isPremiumUser", "hasPremium",
    "isPro", "isProUser", "hasPro",
    "isSubscribed", "isSubscriber", "hasSubscription",
    "isVip", "hasVip",
    "isPaid", "isPaidUser",
    "isUnlocked", "isFullVersion", "hasFullAccess",
    "isEntitled", "hasEntitlement",
)

private val PREMIUM_KEY_MARKERS = setOf(
    "premium", "ispro", "_pro", "subscribed", "subscriber",
    "vip", "unlocked", "full_version", "fullversion",
    "paid", "remove_ads", "no_ads", "pro_user", "entitlement",
)

// ponytail: premium state kept outside SharedPreferences (server flags, native
// storage, encrypted stores) is not covered; upgrade path is tracing each
// app's entitlement source.
private fun isPremiumGetter(method: Method): Boolean =
    method.returnType == "Z" &&
        method.name in PREMIUM_GETTERS &&
        method.parameters.isEmpty() &&
        method.implementation != null

private fun isPreferencesGetBoolean(instruction: Instruction): Boolean {
    val reference = methodReferenceOf(instruction) ?: return false
    return reference.definingClass.contains("SharedPreferences") &&
        reference.name == "getBoolean" &&
        reference.returnType == "Z"
}

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Enables premium features locally: forces premium status getters to report true and premium preference reads to report enabled. Server-side entitlements stay gated.",
    default = false,
) {
    category("Monetization")

    val forceGettersOption = booleanOption(
        key = "forcePremiumGetters",
        default = true,
        title = "Force Premium Getters",
        description = "Make boolean premium status getters return true.",
    )
    val forcePreferencesOption = booleanOption(
        key = "forcePremiumPreferences",
        default = true,
        title = "Force Premium Preferences",
        description = "Force SharedPreferences reads of premium keys to report true.",
    )
    val extraKeysOption = stringOption(
        key = "extraPremiumKeys",
        default = "",
        title = "Extra Premium Keys",
        description = "Comma-separated preference key substrings treated as premium keys.",
        validator = { true },
    )

    execute {
        val forceGetters = forceGettersOption.value == true
        val forcePreferences = forcePreferencesOption.value == true
        if (!forceGetters && !forcePreferences) {
            logger.info("All options disabled.")
            return@execute
        }

        val extraKeys = extraKeysOption.value
            ?.split(",")
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val markers = PREMIUM_KEY_MARKERS + extraKeys
        fun isPremiumKeyLoad(instruction: Instruction): Boolean {
            val key = constStringLoad(instruction)?.lowercase() ?: return false
            return markers.any { it in key }
        }

        val getterPatched = if (forceGetters) {
            patchMatchingMethods(
                isCandidate = ::isPremiumGetter,
            ) { instructions ->
                var count = 0
                // Descending so the inserted return is never revisited.
                for (index in instructions.indices.reversed()) {
                    val instruction = instructions.getOrNull(index) ?: continue
                    if (instruction.opcode == Opcode.RETURN && instruction is OneRegisterInstruction) {
                        replaceInstruction(index, "const/4 v${instruction.registerA}, 0x1")
                        addInstruction(index + 1, "return v${instruction.registerA}")
                        count++
                    }
                }
                count
            }
        } else {
            0
        }

        val preferencePatched = if (forcePreferences) {
            patchMatchingMethods(
                isCandidate = { method ->
                    method.implementation?.instructions?.let { raw ->
                        findConstArgInvokes(raw.toList(), ::isPremiumKeyLoad, ::isPreferencesGetBoolean)
                    }?.isNotEmpty() == true
                },
            ) { instructions ->
                var count = 0
                for (index in findConstArgInvokes(instructions, ::isPremiumKeyLoad, ::isPreferencesGetBoolean)) {
                    if (forceInvokeResult(instructions, index) { register -> "const/4 v$register, 0x1" }) {
                        count++
                    }
                }
                count
            }
        } else {
            0
        }

        logger.info("Applied ${getterPatched + preferencePatched} premium unlock replacements.")
    }
}

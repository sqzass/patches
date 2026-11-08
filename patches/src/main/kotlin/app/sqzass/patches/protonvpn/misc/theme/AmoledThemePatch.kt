/*
 * Copyright (C) 2026 sqzass
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.sqzass.patches.protonvpn.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.sqzass.patches.shared.compat.AppCompatibilities
import app.sqzass.patches.shared.misc.proton.AMOLED_THEME_CLASS
import app.sqzass.patches.shared.misc.proton.amoledBackgroundOverlayPatch
import app.sqzass.patches.shared.misc.proton.indexOfLastWrite
import app.sqzass.patches.shared.misc.proton.injectColorTransformCall
import app.sqzass.patches.shared.misc.proton.markFeaturePatched
import app.sqzass.patches.shared.misc.proton.markPatchMetadata
import app.sqzass.patches.shared.misc.proton.transformCoreDarkBackgrounds
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val SPLASH_STYLE = "ProtonTheme.Splash.Vpn.DarkPreferred"
private const val SPLASH_BACKGROUND_ITEM = "windowSplashScreenBackground"
private const val SPLASH_BACKGROUND_COLOR = "hx_splash_background"
private const val SPLASH_BACKGROUND_COLOR_V31 = "hx_splash_background_v31"

private val SPLASH_BACKGROUND_VALUES = mapOf(
    SPLASH_BACKGROUND_COLOR to mapOf(
        "res/values/colors.xml" to "@color/baltic_sea",
        "res/values-night/colors.xml" to "@android:color/black",
    ),
    SPLASH_BACKGROUND_COLOR_V31 to mapOf(
        "res/values/colors.xml" to "@color/background_norm",
        "res/values-night/colors.xml" to "@android:color/black",
    ),
)

private val SPLASH_STYLE_BACKGROUNDS = mapOf(
    "res/values/styles.xml" to SPLASH_BACKGROUND_COLOR,
    "res/values-v31/styles.xml" to SPLASH_BACKGROUND_COLOR_V31,
)

private val amoledSplashBackgroundPatch = resourcePatch {
    execute {
        SPLASH_BACKGROUND_VALUES.forEach { (color, values) ->
            values.forEach { (file, value) ->
                resourceXml(file, copy = false).use { document ->
                    document.root.appendChild("color") {
                        setAttribute("name", color)
                        text = value
                    }
                }
            }
        }

        SPLASH_STYLE_BACKGROUNDS.forEach { (file, color) ->
            resourceXml(file).use { document ->
                val style = document.root.children.firstOrNull { it.tagName == "style" && it.getAttribute("name") == SPLASH_STYLE }
                    ?: throw IllegalStateException("Style $SPLASH_STYLE not found in $file")
                
                val background = "@color/$color"
                val item = style.children.firstOrNull { it.tagName == "item" && it.getAttribute("name") == SPLASH_BACKGROUND_ITEM }
                
                if (item != null) {
                    item.text = background
                } else {
                    style.appendChild("item") {
                        setAttribute("name", SPLASH_BACKGROUND_ITEM)
                        text = background
                    }
                }
            }
        }
    }
}

@Suppress("unused")
val amoledThemePatch: BytecodePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black and can dim menus, bars and buttons.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch, amoledBackgroundOverlayPatch, amoledSplashBackgroundPatch)

    execute {
        markPatchMetadata("amoledDarkTheme", amoledThemePatch, withDescription = true)
        markFeaturePatched(AMOLED_THEME_CLASS)
        transformCoreDarkBackgrounds()

        CollapsingToolbarColorFingerprint.matchSingle().run {
            val lerpIndex = instructionMatches.last().index
            val collapsedColorRegister = method.getInstruction<FiveRegisterInstruction>(lerpIndex).registerE
            method.injectColorTransformCall(
                method.indexOfLastWrite(collapsedColorRegister, lerpIndex),
                "$AMOLED_THEME_CLASS->transformPackedBackground(J)J",
            )
        }
    }
}
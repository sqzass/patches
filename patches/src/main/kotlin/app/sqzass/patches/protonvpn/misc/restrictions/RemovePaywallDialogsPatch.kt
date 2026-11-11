/* Copyright (C) 2026 sqzass
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.sqzass.patches.protonvpn.misc.restrictions

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.sqzass.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.sqzass.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val removePaywallDialogsPatch: BytecodePatch = bytecodePatch(
    name = "Remove paywall/upgrade dialogs",
    description = "Removes paywall and upgrade dialogs.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        // Paywall dialogs are shown in various activities - intercept the methods that show them
        // Look for methods that display upgrade prompts or paywall UI
        val clazz = mutableClassDefBy("Lcom/protonvpn/android/redesign/app/ui/upsell/TvUpsellActivity;")
        val showMethod = clazz.methods.singleOrNull { it.name == "onCreate" && it.parameterTypes.size == 1 }
            ?: throw IllegalStateException("Upsell activity method not found")
        showMethod.returnEarly()
    }
}

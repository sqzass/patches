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
val spoofPremiumAccountPatch: BytecodePatch = bytecodePatch(
    name = "Spoof premium/Plus account state",
    description = "Spoofs premium/Plus account state to unlock premium features on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        // Spoof the account tier check - find the method that checks if user is premium
        val clazz = mutableClassDefBy("Lcom/protonvpn/android/settings/data/LocalUserSettings;")
        // Check for isPremium() or similar method
        val premiumMethod = clazz.methods.singleOrNull { it.name == "isPremium" && it.parameterTypes.size == 0 }
            ?: findMethodInClazz(clazz, "isPlus")
            ?: throw IllegalStateException("Premium check method not found")
        premiumMethod.returnEarly(true)
    }
}

private fun findMethodInClazz(clazz: MutableClass, name: String): MutableMethod {
    // Helper to find method by name and parameter descriptor
    return clazz.methods.singleOrNull { it.name == name && it.parameterTypes.isEmpty() } ?: throw IllegalStateException("Method not found")
}

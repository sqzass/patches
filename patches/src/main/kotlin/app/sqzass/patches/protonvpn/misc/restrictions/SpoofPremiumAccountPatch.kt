/* Copyright (C) 2026 sqzass
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.sqzass.patches.protonvpn.misc.restrictions

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
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
        val clazz = app.morphe.patcher.patch.classDefBy("Lcom/protonvpn/android/settings/data/LocalUserSettings;")
        // Check for isPremium() or similar method
        val premiumMethod = clazz.methods.singleOrNull { it.name == "isPremium" && it.parameterTypes.size == 0 }
            ?: findMethodInClazz(clazz, "isPlus", "()Z")
            ?: throw IllegalStateException("Premium check method not found")
        premiumMethod.returnEarly(0)
    }
}

private fun findMethodInClazz(clazz: any, name: String, paramDesc: String): any {
    // Helper to find method by name and parameter descriptor
    return clazz.methods.singleOrNull { it.name == name && it.parameterTypes.size == 1 } ?: throw IllegalStateException("Method not found")
}

/* Copyright (C) 2026 sqzass
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.sqzass.patches.protonvpn.misc.restrictions

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.sqzass.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.sqzass.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockTorP2pPatch: BytecodePatch = bytecodePatch(
    name = "Unlock Tor over VPN / P2P",
    description = "Unlocks Tor and P2P servers on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        // Tor over VPN is controlled by isTorEnabled() in ConnectionPreferences
        val clazz = mutableClassDefBy("Lcom/protonvpn/android/connectionpreferences/ConnectionPreferences;")
        val method = clazz.methods.singleOrNull { it.name == "isTorEnabled" && it.parameterTypes.isEmpty() }
            ?: throw IllegalStateException("isTorEnabled not found")
        method.returnEarly(true)

        // P2P is controlled by isP2PEnabled() in ConnectionPreferences
        val p2pMethod = clazz.methods.singleOrNull { it.name == "isP2PEnabled" && it.parameterTypes.isEmpty() }
            ?: throw IllegalStateException("isP2PEnabled not found")
        p2pMethod.returnEarly(true)
    }
}

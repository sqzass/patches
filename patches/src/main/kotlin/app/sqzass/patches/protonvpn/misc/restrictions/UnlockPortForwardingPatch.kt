/* Copyright (C) 2026 sqzass
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.sqzass.patches.protonvpn.misc.restrictions

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.sqzass.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPortForwardingPatch: BytecodePatch = bytecodePatch(
    name = "Unlock Port Forwarding",
    description = "Unlocks Port Forwarding on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        // Port Forwarding is controlled by isPortForwardingEnabled() in ConnectionPreferences
        val clazz = app.morphe.patcher.patch.classDefBy("Lcom/protonvpn/android/connectionpreferences/ConnectionPreferences;")
        val method = clazz.methods.singleOrNull { it.name == "isPortForwardingEnabled" && it.parameterTypes.size == 0 }
            ?: throw IllegalStateException("isPortForwardingEnabled not found")
        method.returnEarly(0)
    }
}

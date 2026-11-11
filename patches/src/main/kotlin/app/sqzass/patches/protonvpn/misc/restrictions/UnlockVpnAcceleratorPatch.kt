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
val unlockVpnAcceleratorPatch: BytecodePatch = bytecodePatch(
    name = "Unlock VPN Accelerator",
    description = "Unlocks VPN Accelerator on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        // VPN Accelerator is controlled by isAcceleratorEnabled() in ConnectionPreferences
        val clazz = mutableClassDefBy("Lcom/protonvpn/android/connectionpreferences/ConnectionPreferences;")
        val method = clazz.methods.singleOrNull { it.name == "isAcceleratorEnabled" && it.parameterTypes.isEmpty() }
            ?: throw IllegalStateException("isAcceleratorEnabled not found")
        method.returnEarly(true)
    }
}

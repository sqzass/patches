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
val unlockSpeedTestPatch: BytecodePatch = bytecodePatch(
    name = "Unlock speed test",
    description = "Unlocks speed test feature.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        // Speed test is controlled by a feature flag - intercept the method that checks it
        val clazz = mutableClassDefBy("Lcom/protonvpn/android/redesign/app/ui/speed/SpeedTestManager;")
        val speedMethod = clazz.methods.singleOrNull { it.name == "isSpeedTestEnabled" && it.parameterTypes.isEmpty() }
            ?: clazz.methods.singleOrNull { it.name == "checkSpeed" && it.parameterTypes.size == 1 }
            ?: throw IllegalStateException("Speed test method not found")
        speedMethod.returnEarly(true)
    }
}


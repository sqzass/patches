/* Copyright (C) 2026 sqzass
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.sqzass.patches.protonvpn.misc.restrictions

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.util.returnEarly
import app.sqzass.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.sqzass.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val disableForcedUpdatePatch: BytecodePatch = bytecodePatch(
    name = "Disable forced update",
    description = "Disables forced-update gate.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        val clazz = mutableClassDefBy("Lme/proton/core/presentation/ui/alert/ForceUpdateActivity;")
        val method = clazz.methods.firstOrNull { it.name == "shouldForceUpdate" || it.name == "checkUpdate" }
            ?: throw IllegalStateException("Force update method not found")
        
        // This cleanly replaces the method body to just return false
        method.returnEarly(false)
    }
}

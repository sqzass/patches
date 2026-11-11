/*
 * Copyright (C) 2026 sqzass
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
val unlockPlusServersPatch: BytecodePatch = bytecodePatch(
    name = "Unlock Plus servers",
    description = "Forces Server.isPlusServer() to return true so Secure Core / Plus-tier servers are selectable on free plans.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        val clazz = mutableClassDefBy("Lcom/protonvpn/android/servers/Server;")
        val method = clazz.methods.singleOrNull { it.name == "isPlusServer" && it.parameterTypes.isEmpty() }
            ?: throw IllegalStateException("isPlusServer not found")
        method.returnEarly(true)
    }
}

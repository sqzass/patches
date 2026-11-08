/* Copyright (C) 2026 sqzass
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.sqzass.patches.protonvpn.misc.restrictions

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.sqzass.patches.protonvpn.misc.settings.patchesSettingsPatch
import app.sqzass.patches.shared.compat.AppCompatities

@Suppress("unused")
val removeAllTelemetryPatch: BytecodePatch = bytecodePatch(
    name = "Remove all telemetry",
    description = "Removes all telemetry/analytics.",
) {
    compatibleWith(AppCompatities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        // Telemetry is sent via me.proton.core telemetry library
        // Intercept the methods that send telemetry data
        val clazz = app.morphe.patcher.patch.classDefBy("Lme/proton/core/telemetry/TelemetryManager;")
        val sendMethod = clazz.methods.singleOrNull { it.name == "send" && it.parameterTypes.size == 1 }
            ?: clazz.methods.singleOrNull { it.name == "report" && it.parameterTypes.size == 1 }
            ?: throw IllegalStateException("Telemetry method not found")
        // Replace the method body to return void
        sendMethod.returnEarly(0)
    }
}

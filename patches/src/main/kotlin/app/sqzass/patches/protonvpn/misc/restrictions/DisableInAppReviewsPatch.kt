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
val disableInAppReviewsPatch: BytecodePatch = bytecodePatch(
    name = "Disable in-app reviews",
    description = "Disables in-app review prompts.",
) {
    compatibleWith(AppCompatibilities.PROTON_VPN)
    dependsOn(patchesSettingsPatch)

    execute {
        // In-app reviews are triggered by review prompts - intercept the method that shows them
        val clazz = mutableClassDefBy("Lcom/protonvpn/android/redesign/app/ui/review/ReviewPromptHandler;")
        val showMethod = clazz.methods.singleOrNull { it.name == "showReviewPrompt" && it.parameterTypes.isEmpty() }
            ?: findMethodInClazz(clazz, "requestReview")
            ?: throw IllegalStateException("Review prompt method not found")
        showMethod.returnEarly()
    }
}

private fun findMethodInClazz(clazz: MutableClass, name: String): MutableMethod {
    return clazz.methods.singleOrNull { it.name == name && it.parameterTypes.isEmpty() } ?: throw IllegalStateException("Method not found")
}

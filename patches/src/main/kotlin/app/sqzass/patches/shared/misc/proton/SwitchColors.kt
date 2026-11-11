/* Copyright (C) 2026 sqzass SPDX-License-Identifier: GPL-3.0-only */
package app.sqzass.patches.shared.misc.proton

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private const val SWITCH_COLORS_STYLE = "ThemeOverlay.Patches.MaterialSwitch"

private val SWITCH_COLOR_ROLES = mapOf(
    "colorOnPrimary" to "?attr/proton_text_inverted",
    "colorPrimaryContainer" to "?attr/colorOnPrimary",
    "colorOnPrimaryContainer" to "?attr/colorPrimary",
    "colorOutline" to "?attr/proton_icon_hint",
    "colorOnSurfaceVariant" to "?attr/proton_icon_hint",
    "colorSurfaceVariant" to "?attr/proton_background_norm",
    "colorSurfaceContainerHighest" to "?attr/proton_background_norm",
)

internal val switchColorsPatch = resourcePatch {
    execute {
        val doc = document("res/values/attrs.xml")
        val declaredAttributes = mutableSetOf<String>()
        for (i in 0 until doc.getElementsByTagName("attr").length) {
            val element = doc.getElementsByTagName("attr").item(i) as Element
            declaredAttributes.add(element.getAttribute("name"))
        }
        val style = document("res/values/styles.xml").use { document ->
            val s = document.createElement("style")
            s.setAttribute("name", SWITCH_COLORS_STYLE)
            s.setAttribute("parent", "")
            SWITCH_COLOR_ROLES.filterKeys { it in declaredAttributes }.forEach { (role, color) ->
                val item = document.createElement("item")
                item.setAttribute("name", role)
                item.textContent = color
                s.appendChild(item)
            }
            document.documentElement.appendChild(s)
        }
    }
}
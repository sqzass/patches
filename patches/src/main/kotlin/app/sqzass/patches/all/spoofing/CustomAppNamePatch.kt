package app.sqzass.patches.all.spoofing

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.sqzass.patches.shared.collectElements
import java.util.logging.Logger

private val logger = Logger.getLogger("CustomAppNamePatch")

@Suppress("unused")
val customAppNamePatch = resourcePatch(
    name = "Custom app name",
    description = "Replaces the application label shown in the launcher with a custom name.",
    default = false,
) {
    category("Interface")

    val appNameOption = stringOption(
        key = "appName",
        default = "",
        title = "App Name",
        description = "Label shown in the launcher. Leave empty to keep the app's own label.",
    )
    val overrideActivityLabelsOption = booleanOption(
        key = "overrideActivityLabels",
        default = true,
        title = "Override Activity Labels",
        description = "Also replace android:label on activities and activity aliases that declare their own.",
    )

    execute {
        val appName = appNameOption.value?.trim().orEmpty()
        if (appName.isEmpty()) {
            logger.info("No custom app name set.")
            return@execute
        }

        document("AndroidManifest.xml").use { document ->
            val appNodes = document.getElementsByTagName("application")
            if (appNodes.length == 0) {
                logger.info("No <application> element found in manifest.")
                return@use
            }

            (appNodes.item(0) as? org.w3c.dom.Element)?.setAttribute("android:label", appName)
            logger.info("Set application label to \"$appName\".")

            if (overrideActivityLabelsOption.value == true) {
                val labels = collectElements(document, listOf("activity", "activity-alias")) { element ->
                    element.hasAttribute("android:label")
                }
                for (element in labels) {
                    element.setAttribute("android:label", appName)
                }
                logger.info("Overrode ${labels.size} activity labels.")
            }
        }
    }
}
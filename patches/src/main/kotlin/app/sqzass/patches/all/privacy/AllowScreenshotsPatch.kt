package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.collectElements
import java.util.logging.Logger

private val logger = Logger.getLogger("AllowScreenshotsPatch")

@Suppress("unused")
val allowScreenshotsPatch = resourcePatch(
    name = "Allow screenshots and screen recording",
    description = "Removes recent-apps preview hiding and screen capture restrictions across application activities.",
    default = false
) {
    category("Interface")

    val enableRecentPreviewsOption = booleanOption(
        key = "enableRecentPreviews",
        default = true,
        title = "Enable Recent App Previews",
        description = "Removes android:excludeFromRecents to allow viewing the app thumbnail in the Android app switcher.",
    )

    val removeCaptureRestrictionsOption = booleanOption(
        key = "removeCaptureRestrictions",
        default = true,
        title = "Remove Capture Restrictions",
        description = "Clears screenshot and screen recording blocking flags from activities.",
    )

    execute {
        document("AndroidManifest.xml").use { document ->
            val activities = collectElements(document, listOf("activity", "activity-alias")) { true }

            for (activity in activities) {
                if (enableRecentPreviewsOption.value == true) {
                    if (activity.hasAttribute("android:excludeFromRecents")) {
                        activity.removeAttribute("android:excludeFromRecents")
                    }
                }

                if (removeCaptureRestrictionsOption.value == true) {
                    // Clear any screen capture callback restrictions
                    if (activity.hasAttribute("android:screenCaptureCallback")) {
                        activity.removeAttribute("android:screenCaptureCallback")
                    }
                }
            }

            logger.info("Successfully updated activities for screenshot and recording compatibility.")
        }
    }
}

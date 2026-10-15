package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.collectElements
import java.util.logging.Logger

private val logger = Logger.getLogger("UnlockOrientationPatch")

@Suppress("unused")
val unlockOrientationPatch = resourcePatch(
    name = "Unlock screen orientation",
    description = "Forces orientation unlock on apps that force an orientation.",
    default = false
) {
    category("Interface")

    val removeOrientationLocksOption = booleanOption(
        key = "removeOrientationLocks",
        default = true,
        title = "Remove Orientation Locks",
        description = "Removes android:screenOrientation attributes from activities.",
    )

    execute {
        if (removeOrientationLocksOption.value != true) {
            logger.info("Orientation lock removal disabled.")
            return@execute
        }

        document("AndroidManifest.xml").use { document ->
            val lockedActivities = collectElements(document, listOf("activity")) { element ->
                element.hasAttribute("android:screenOrientation")
            }
            for (element in lockedActivities) {
                element.removeAttribute("android:screenOrientation")
            }
            logger.info("Removed android:screenOrientation from ${lockedActivities.size} activities.")
        }
    }
}
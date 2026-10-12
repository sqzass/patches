package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private val logger = Logger.getLogger("EnableAppBackupsPatch")

@Suppress("unused")
val enableAppBackupsPatch = resourcePatch(
    name = "Enable app backups",
    description = "Allows the app to back up its data by enabling android:allowBackup and removing restrictive backup rules in the application manifest.",
    default = false
) {
    category("General")

    val enableBackupsOption = booleanOption(
        key = "enableBackups",
        default = true,
        title = "Enable Backups",
        description = "Sets android:allowBackup to true on the application tag.",
    )

    val removeBackupRulesOption = booleanOption(
        key = "removeBackupRules",
        default = true,
        title = "Remove Backup Rules",
        description = "Removes android:fullBackupContent and android:dataExtractionRules so the system default rules (back up everything) apply.",
    )

    execute {
        if (enableBackupsOption.value != true && removeBackupRulesOption.value != true) {
            logger.info("All backup options disabled.")
            return@execute
        }

        document("AndroidManifest.xml").use { document ->
            val appElement = document.getElementsByTagName("application").item(0) as? Element
            if (appElement == null) {
                logger.info("No <application> element found in manifest.")
                return@use
            }

            if (enableBackupsOption.value == true) {
                appElement.setAttribute("android:allowBackup", "true")
                logger.info("Set android:allowBackup to true")
            }

            if (removeBackupRulesOption.value == true) {
                // ponytail: dropping the rules falls back to the system default of
                // backing up everything; cloud/OEM transports may still exclude data
                // via their own policy. Upgrade path: write explicit permissive
                // fullBackupContent/dataExtractionRules XML instead.
                var removed = 0
                for (attribute in listOf("android:fullBackupContent", "android:dataExtractionRules")) {
                    if (appElement.hasAttribute(attribute)) {
                        appElement.removeAttribute(attribute)
                        removed++
                    }
                }
                logger.info("Removed $removed backup rule attributes.")
            }
        }
    }
}
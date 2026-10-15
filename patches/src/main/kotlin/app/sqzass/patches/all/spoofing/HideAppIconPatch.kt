package app.sqzass.patches.all.spoofing

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.collectDescendants
import app.sqzass.patches.shared.collectElements
import java.util.logging.Logger

private val logger = Logger.getLogger("HideAppIconPatch")

private const val MAIN_ACTION = "android.intent.action.MAIN"
private const val LAUNCHER_CATEGORY = "android.intent.category.LAUNCHER"

@Suppress("unused")
val hideAppIconPatch = resourcePatch(
    name = "Hide app icon",
    description = "Removes MAIN/LAUNCHER intent filters so the app no longer appears in launcher app lists.",
    default = false,
) {
    category("Interface")

    val removeLauncherFiltersOption = booleanOption(
        key = "removeLauncherFilters",
        default = true,
        title = "Remove Launcher Entry",
        description = "Strip intent filters that combine android.intent.action.MAIN with the LAUNCHER category.",
    )

    execute {
        if (removeLauncherFiltersOption.value != true) {
            logger.info("Launcher filter removal disabled.")
            return@execute
        }

        document("AndroidManifest.xml").use { document ->
            // Only the intent-filter is removed, never the activity itself:
            // the component keeps working and stays reachable from app info,
            // shortcuts, and any secondary entry points it declares.
            val filters = collectElements(document, listOf("activity", "activity-alias")) { true }
                .flatMap { owner -> collectDescendants(owner, "intent-filter") }

            var removed = 0
            for (filter in filters) {
                val hasMain = collectDescendants(filter, "action").any {
                    it.getAttribute("android:name") == MAIN_ACTION
                }
                val hasLauncher = collectDescendants(filter, "category").any {
                    it.getAttribute("android:name") == LAUNCHER_CATEGORY
                }
                if (hasMain && hasLauncher) {
                    filter.parentNode?.removeChild(filter)
                    removed++
                }
            }

            logger.info("Removed $removed launcher intent filters.")
        }
    }
}
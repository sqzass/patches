package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.collectDescendants
import app.sqzass.patches.shared.collectElements
import java.util.logging.Logger

private val logger = Logger.getLogger("DisableBootStartPatch")

private val BOOT_ACTIONS = setOf(
    "android.intent.action.BOOT_COMPLETED",
    "android.intent.action.LOCKED_BOOT_COMPLETED",
    "android.intent.action.QUICKBOOT_POWERON",
    "com.htc.intent.action.QUICKBOOT_POWERON",
)

@Suppress("unused")
val disableBootStartPatch = resourcePatch(
    name = "Disable boot start-up",
    description = "Removes boot intent filters so the app cannot start itself when the device boots.",
    default = false
) {
    category("Privacy")

    val removeBootFiltersOption = booleanOption(
        key = "removeBootFilters",
        default = true,
        title = "Remove Boot Intent Filters",
        description = "Strip BOOT_COMPLETED, LOCKED_BOOT_COMPLETED, and quickboot intent filters from receivers and services.",
    )

    execute {
        if (removeBootFiltersOption.value != true) {
            logger.info("Boot intent filter removal disabled.")
            return@execute
        }

        document("AndroidManifest.xml").use { document ->
            val filters = collectElements(document, listOf("receiver", "service")) { true }
                .flatMap { owner -> collectDescendants(owner, "intent-filter") }

            var removed = 0
            for (filter in filters) {
                val isBootFilter = collectDescendants(filter, "action").any { action ->
                    action.getAttribute("android:name") in BOOT_ACTIONS
                }
                if (isBootFilter) {
                    filter.parentNode?.removeChild(filter)
                    removed++
                }
            }

            logger.info("Removed $removed boot intent filters.")
        }
    }
}
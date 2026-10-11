package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.PERMISSION_TAGS
import app.sqzass.patches.shared.removeElements
import java.util.logging.Logger

private val logger = Logger.getLogger("StripPackageVisibilityPatch")

@Suppress("unused")
val stripPackageVisibilityPatch = resourcePatch(
    name = "Strip package visibility",
    description = "Removes package visibility declarations so the app cannot enumerate other installed applications.",
    default = false
) {
    category("Privacy")

    val removeQueriesOption = booleanOption(
        key = "removeQueries",
        default = true,
        title = "Remove Queries Declarations",
        description = "Remove all <queries> elements used to probe for other installed packages.",
    )

    val removeQueryAllPackagesOption = booleanOption(
        key = "removeQueryAllPackages",
        default = true,
        title = "Remove QUERY_ALL_PACKAGES Permission",
        description = "Remove the QUERY_ALL_PACKAGES permission that grants blanket visibility of installed apps.",
    )

    execute {
        document("AndroidManifest.xml").use { document ->
            if (removeQueriesOption.value == true) {
                val removed = removeElements(document, listOf("queries")) { true }
                logger.info("Removed ${removed.size} <queries> elements.")
            }

            if (removeQueryAllPackagesOption.value == true) {
                val removed = removeElements(document, PERMISSION_TAGS) { element ->
                    element.getAttribute("android:name") == "android.permission.QUERY_ALL_PACKAGES"
                }
                if (removed.isNotEmpty()) {
                    logger.info("Removed permissions: $removed")
                }
            }
        }
    }
}
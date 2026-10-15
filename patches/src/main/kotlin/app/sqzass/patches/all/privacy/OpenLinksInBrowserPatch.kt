package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.collectDescendants
import app.sqzass.patches.shared.collectElements
import org.w3c.dom.Element
import java.util.logging.Logger

private val logger = Logger.getLogger("OpenLinksInBrowserPatch")

private const val VIEW_ACTION = "android.intent.action.VIEW"

private fun Element.isHttpLinkFilter(): Boolean {
    val handlesView = collectDescendants(this, "action").any {
        it.getAttribute("android:name") == VIEW_ACTION
    }
    if (!handlesView) return false

    return collectDescendants(this, "data").any { data ->
        val scheme = data.getAttribute("android:scheme").lowercase()
        scheme == "http" || scheme == "https"
    }
}

@Suppress("unused")
val openLinksInBrowserPatch = resourcePatch(
    name = "Open links in browser",
    description = "Stops http and https links from opening inside the app.",
    default = false
) {
    category("Interface")

    val removeAutoVerifyOption = booleanOption(
        key = "removeAutoVerify",
        default = true,
        title = "Remove App Link Verification",
        description = "Remove android:autoVerify from intent filters.",
    )

    val removeLinkFiltersOption = booleanOption(
        key = "removeLinkFilters",
        default = false,
        title = "Remove Link Intent Filters",
        description = "Remove intent filters that handle http/https links.",
    )

    execute {
        document("AndroidManifest.xml").use { document ->
            val intentFilters = collectElements(document, listOf("activity", "activity-alias")) { true }
                .flatMap { activity -> collectDescendants(activity, "intent-filter") }

            val removedFilters = mutableListOf<Element>()
            var removedAutoVerify = 0

            for (filter in intentFilters) {
                if (removeLinkFiltersOption.value == true && filter.isHttpLinkFilter()) {
                    removedFilters += filter
                    continue
                }
                if (removeAutoVerifyOption.value == true && filter.hasAttribute("android:autoVerify")) {
                    filter.removeAttribute("android:autoVerify")
                    removedAutoVerify++
                }
            }

            for (filter in removedFilters) {
                filter.parentNode?.removeChild(filter)
            }

            logger.info(
                "Removed ${removedFilters.size} link intent filters " +
                    "and $removedAutoVerify android:autoVerify attributes."
            )
        }
    }
}
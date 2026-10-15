package app.sqzass.patches.all.integrity

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.collectDescendants
import app.sqzass.patches.shared.collectElements
import java.util.logging.Logger

private val logger = Logger.getLogger("DisablePlayProtectPatch")

private const val PACKAGE_NEEDS_VERIFICATION = "android.intent.action.PACKAGE_NEEDS_VERIFICATION"

private val VERIFIER_ROLES = setOf(
    "android.app.role.PACKAGE_VERIFIER",
    "android.app.role.SYSTEM_PACKAGE_VERIFIER",
)

// ponytail: Play Protect blocking of re-signed APKs happens inside Google
// Play services, not in the patched APK; this only strips the APK-side verify
// hooks. Upgrade path is none on-device — the ceiling lives in GMS.
@Suppress("unused")
val disablePlayProtectPatch = resourcePatch(
    name = "Disable Play Protect hooks",
    description = "Strips APK-side verify hooks: removes package verification intent filters and verifier roles. Play Protect verdicts computed inside Google Play services cannot be changed from the app.",
    default = false,
) {
    category("Integrity")

    val stripVerificationFiltersOption = booleanOption(
        key = "stripVerificationFilters",
        default = true,
        title = "Strip Verification Filters",
        description = "Remove PACKAGE_NEEDS_VERIFICATION intent filters from receivers.",
    )
    val stripVerifierRolesOption = booleanOption(
        key = "stripVerifierRoles",
        default = true,
        title = "Strip Verifier Roles",
        description = "Remove package verifier roles from the manifest.",
    )

    execute {
        val stripFilters = stripVerificationFiltersOption.value == true
        val stripRoles = stripVerifierRolesOption.value == true
        if (!stripFilters && !stripRoles) {
            logger.info("All options disabled.")
            return@execute
        }

        document("AndroidManifest.xml").use { document ->
            var filters = 0
            if (stripFilters) {
                val matches = collectElements(document, listOf("receiver")) { true }
                    .flatMap { receiver -> collectDescendants(receiver, "intent-filter") }
                    .filter { filter ->
                        collectDescendants(filter, "action").any {
                            it.getAttribute("android:name") == PACKAGE_NEEDS_VERIFICATION
                        }
                    }
                for (filter in matches) {
                    filter.parentNode?.removeChild(filter)
                    filters++
                }
            }

            var roles = 0
            if (stripRoles) {
                val matches = collectElements(document, listOf("role")) { element ->
                    element.getAttribute("android:name") in VERIFIER_ROLES
                }
                for (element in matches) {
                    element.parentNode?.removeChild(element)
                    roles++
                }
            }

            logger.info("Removed $filters verification filters and $roles verifier roles.")
        }
    }
}

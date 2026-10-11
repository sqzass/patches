package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.PERMISSION_TAGS
import app.sqzass.patches.shared.TrackerPrefixes
import app.sqzass.patches.shared.removeElements
import org.w3c.dom.Element
import java.util.logging.Logger

private val logger = Logger.getLogger("StripTrackersPatch")

private val AD_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
)

@Suppress("unused")
val stripTrackersPatch = resourcePatch(
    name = "Strip trackers and telemetry",
    description = "Strips known tracking, analytics, and advertising SDK components (such as Google Analytics, Firebase, AppsFlyer, Adjust, Facebook, and Crashlytics) from the application manifest.",
    default = false
) {
    category("Privacy")

    val stripGoogleOption = booleanOption(
        key = "stripGoogleAnalytics",
        default = true,
        title = "Strip Google & Firebase Analytics",
        description = "Remove Google Analytics, Firebase Analytics, and AppMeasurement components.",
    )

    val stripFacebookOption = booleanOption(
        key = "stripFacebookAnalytics",
        default = true,
        title = "Strip Facebook Analytics",
        description = "Remove Facebook App Events and analytics provider components.",
    )

    val stripAttributionOption = booleanOption(
        key = "stripAttributionTrackers",
        default = true,
        title = "Strip Attribution Trackers",
        description = "Remove third-party attribution and tracking SDKs (AppsFlyer, Adjust, Branch, Kochava, Mixpanel, Flurry).",
    )

    val stripCrashOption = booleanOption(
        key = "stripCrashReporting",
        default = true,
        title = "Strip Crash Reporting Telemetry",
        description = "Remove Crashlytics and Microsoft App Center telemetry services.",
    )

    val stripAdIdOption = booleanOption(
        key = "stripAdvertisingId",
        default = true,
        title = "Strip Advertising ID Permissions",
        description = "Remove AD_ID and AdServices permissions to prevent advertising tracking.",
    )

    execute {
        val activePrefixes = mutableSetOf<String>()

        if (stripGoogleOption.value == true) {
            activePrefixes.addAll(TrackerPrefixes.GOOGLE)
        }
        if (stripFacebookOption.value == true) {
            activePrefixes.addAll(TrackerPrefixes.FACEBOOK)
        }
        if (stripAttributionOption.value == true) {
            activePrefixes.addAll(TrackerPrefixes.ATTRIBUTION)
        }
        if (stripCrashOption.value == true) {
            activePrefixes.addAll(TrackerPrefixes.CRASH)
        }

        document("AndroidManifest.xml").use { document ->
            var removedComponents = emptyList<String>()

            if (activePrefixes.isNotEmpty()) {
                removedComponents = removeElements(
                    document,
                    listOf("service", "receiver", "provider", "activity"),
                ) { element ->
                    activePrefixes.any { prefix -> element.getAttribute("android:name").startsWith(prefix) }
                }
            }

            // Strip ad-related permissions
            if (stripAdIdOption.value == true) {
                val removedPerms = removeElements(document, PERMISSION_TAGS) { element ->
                    AD_PERMISSIONS.contains(element.getAttribute("android:name"))
                }
                if (removedPerms.isNotEmpty()) {
                    logger.info("Stripped ad permissions: $removedPerms")
                }
            }

            // Disable Firebase and Google Analytics collection flags in meta-data
            if (stripGoogleOption.value == true) {
                val metaNodes = document.getElementsByTagName("meta-data")
                for (i in 0 until metaNodes.length) {
                    val meta = metaNodes.item(i) as? Element ?: continue
                    val metaName = meta.getAttribute("android:name")
                    if (metaName == "firebase_analytics_collection_enabled" ||
                        metaName == "google_analytics_default_allow_analytics_storage" ||
                        metaName == "google_analytics_default_allow_ad_storage"
                    ) {
                        meta.setAttribute("android:value", "false")
                        logger.info("Set meta-data $metaName to false")
                    }
                }
            }

            logger.info("Successfully stripped ${removedComponents.size} tracker components: $removedComponents")
        }
    }
}

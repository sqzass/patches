package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

private val logger = Logger.getLogger("StripTrackersPatch")

private val GOOGLE_TRACKER_PREFIXES = setOf(
    "com.google.android.gms.measurement.",
    "com.google.firebase.analytics.",
    "com.google.android.gms.analytics.",
    "com.google.android.gms.ads.",
)

private val FACEBOOK_TRACKER_PREFIXES = setOf(
    "com.facebook.appevents.",
    "com.facebook.internal.FacebookInitProvider",
    "com.facebook.marketing.",
)

private val ATTRIBUTION_TRACKER_PREFIXES = setOf(
    "com.appsflyer.",
    "com.adjust.sdk.",
    "io.branch.",
    "com.kochava.",
    "com.mixpanel.",
    "com.flurry.",
)

private val CRASH_TRACKER_PREFIXES = setOf(
    "com.google.firebase.crashlytics.",
    "com.crashlytics.",
    "com.microsoft.appcenter.",
)

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
            activePrefixes.addAll(GOOGLE_TRACKER_PREFIXES)
        }
        if (stripFacebookOption.value == true) {
            activePrefixes.addAll(FACEBOOK_TRACKER_PREFIXES)
        }
        if (stripAttributionOption.value == true) {
            activePrefixes.addAll(ATTRIBUTION_TRACKER_PREFIXES)
        }
        if (stripCrashOption.value == true) {
            activePrefixes.addAll(CRASH_TRACKER_PREFIXES)
        }

        document("AndroidManifest.xml").use { document ->
            val componentTags = listOf("service", "receiver", "provider", "activity")
            val removedComponents = mutableListOf<String>()

            if (activePrefixes.isNotEmpty()) {
                for (tag in componentTags) {
                    val nodes = document.getElementsByTagName(tag)
                    val elementsToRemove = mutableListOf<Element>()

                    for (i in 0 until nodes.length) {
                        val element = nodes.item(i) as? Element ?: continue
                        val name = element.getAttribute("android:name")
                        if (activePrefixes.any { prefix -> name.startsWith(prefix) }) {
                            elementsToRemove.add(element)
                            removedComponents.add(name)
                        }
                    }

                    for (element in elementsToRemove) {
                        element.parentNode?.removeChild(element)
                    }
                }
            }

            // Strip ad-related permissions
            if (stripAdIdOption.value == true) {
                val permTags = listOf("uses-permission", "uses-permission-sdk-23")
                val removedPerms = mutableListOf<String>()

                for (tag in permTags) {
                    val nodes = document.getElementsByTagName(tag)
                    val elementsToRemove = mutableListOf<Element>()

                    for (i in 0 until nodes.length) {
                        val element = nodes.item(i) as? Element ?: continue
                        val name = element.getAttribute("android:name")
                        if (AD_PERMISSIONS.contains(name)) {
                            elementsToRemove.add(element)
                            removedPerms.add(name)
                        }
                    }

                    for (element in elementsToRemove) {
                        element.parentNode?.removeChild(element)
                    }
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

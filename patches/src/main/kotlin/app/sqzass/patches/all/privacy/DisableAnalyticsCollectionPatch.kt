package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.collectElements
import java.util.logging.Logger
import org.w3c.dom.Element

private val logger = Logger.getLogger("DisableAnalyticsCollectionPatch")

@Suppress("unused")
val disableAnalyticsCollectionPatch = resourcePatch(
    name = "Disable analytics collection",
    description = "Disables Firebase Analytics, Crashlytics, and Performance Monitoring data collection.",
    default = false
) {
    category("Privacy")

    val disableFirebaseAnalyticsOption = booleanOption(
        key = "disableFirebaseAnalytics",
        default = true,
        title = "Disable Firebase Analytics",
        description = "Force firebase_analytics_collection_enabled to false.",
    )

    val disableCrashlyticsOption = booleanOption(
        key = "disableCrashlytics",
        default = true,
        title = "Disable Crashlytics",
        description = "Force firebase_crashlytics_collection_enabled to false.",
    )

    val disablePerformanceOption = booleanOption(
        key = "disablePerformanceMonitoring",
        default = true,
        title = "Disable Performance Monitoring",
        description = "Force firebase_performance_collection_enabled to false.",
    )

    execute {
        val flags = buildList {
            if (disableFirebaseAnalyticsOption.value == true) add("firebase_analytics_collection_enabled")
            if (disableCrashlyticsOption.value == true) add("firebase_crashlytics_collection_enabled")
            if (disablePerformanceOption.value == true) add("firebase_performance_collection_enabled")
        }

        if (flags.isEmpty()) {
            logger.info("All analytics collection options disabled.")
            return@execute
        }

        document("AndroidManifest.xml").use { document ->
            val appElement = document.getElementsByTagName("application").item(0) as? Element
            if (appElement == null) {
                logger.info("No <application> element found in manifest.")
                return@use
            }

            val existingFlags = collectElements(document, listOf("meta-data")) { element ->
                flags.contains(element.getAttribute("android:name"))
            }
            val presentFlags = existingFlags.mapTo(mutableSetOf()) {
                it.getAttribute("android:name")
            }

            for (meta in existingFlags) {
                meta.setAttribute("android:value", "false")
                logger.info("Set meta-data ${meta.getAttribute("android:name")} to false")
            }

            for (flag in flags) {
                if (flag in presentFlags) continue
                val created = document.createElement("meta-data")
                created.setAttribute("android:name", flag)
                created.setAttribute("android:value", "false")
                appElement.appendChild(created)
                logger.info("Added meta-data $flag=false")
            }
        }
    }
}
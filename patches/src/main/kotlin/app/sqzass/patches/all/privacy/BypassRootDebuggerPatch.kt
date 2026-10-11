package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

private val logger = Logger.getLogger("BypassRootDebuggerPatch")

@Suppress("unused")
val bypassRootDebuggerPatch = resourcePatch(
    name = "Enable debugging and inspection",
    description = "Enables application debugging flags, permits cleartext traffic for proxy inspection, and strips test-only restrictions in the application manifest.",
    default = false
) {
    category("Debugging")

    val enableDebuggingOption = booleanOption(
        key = "enableDebugging",
        default = true,
        title = "Enable Debugging",
        description = "Sets android:debuggable to true on the application tag to allow attaching debuggers and profiling tools.",
    )

    val allowCleartextTrafficOption = booleanOption(
        key = "allowCleartextTraffic",
        default = true,
        title = "Allow Cleartext Traffic",
        description = "Sets android:usesCleartextTraffic to true to allow inspecting HTTP/cleartext network communication.",
    )

    val removeTestOnlyOption = booleanOption(
        key = "removeTestOnly",
        default = true,
        title = "Remove Test-Only Flag",
        description = "Removes android:testOnly if present so the application can be installed normally on production devices.",
    )

    execute {
        document("AndroidManifest.xml").use { document ->
            val appNodes = document.getElementsByTagName("application")
            if (appNodes.length > 0) {
                val appElement = appNodes.item(0) as? Element
                if (appElement != null) {
                    if (enableDebuggingOption.value == true) {
                        appElement.setAttribute("android:debuggable", "true")
                        logger.info("Set android:debuggable to true")
                    }

                    if (allowCleartextTrafficOption.value == true) {
                        appElement.setAttribute("android:usesCleartextTraffic", "true")
                        logger.info("Set android:usesCleartextTraffic to true")
                    }

                    if (removeTestOnlyOption.value == true) {
                        if (appElement.hasAttribute("android:testOnly")) {
                            appElement.removeAttribute("android:testOnly")
                            logger.info("Removed android:testOnly attribute")
                        }
                    }
                }
            }
        }
    }
}

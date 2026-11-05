package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

private val logger = Logger.getLogger("EnableDebuggingPatch")

private const val CONFIG_PATH = "res/xml/sqz_network_security_config.xml"
private const val CONFIG_REF = "@xml/sqz_network_security_config"

@Suppress("unused")
val enableDebuggingPatch = resourcePatch(
    name = "Enable debugging and inspection",
    description = "Enables debugging flags, trusts user CA certificates, permits cleartext traffic for proxy inspection, and strips test-only restrictions in the application manifest.",
    default = false
) {
    category("Debugging")

    val enableDebuggingOption = booleanOption(
        key = "enableDebugging",
        default = true,
        title = "Enable Debugging",
        description = "Sets android:debuggable to true on the application tag.",
    )

    val trustUserCertsOption = booleanOption(
        key = "trustUserCerts",
        default = true,
        title = "Trust User Certificates",
        description = "Accept TLS certificates signed by user-installed certificate authorities.",
    )

    val allowCleartextTrafficOption = booleanOption(
        key = "allowCleartextTraffic",
        default = true,
        title = "Allow Cleartext Traffic",
        description = "Sets android:usesCleartextTraffic to true and permits cleartext traffic in the network security config.",
    )

    val removeTestOnlyOption = booleanOption(
        key = "removeTestOnly",
        default = true,
        title = "Remove Test-Only Flag",
        description = "Removes android:testOnly if present.",
    )

    execute {
        val enableDebugging = enableDebuggingOption.value == true
        val trustUserCerts = trustUserCertsOption.value == true
        val allowCleartext = allowCleartextTrafficOption.value == true
        val removeTestOnly = removeTestOnlyOption.value == true

        if (!enableDebugging && !trustUserCerts && !allowCleartext && !removeTestOnly) {
            logger.info("All debugging options disabled.")
            return@execute
        }

        if (trustUserCerts || allowCleartext) {
            val config = buildString {
                appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
                appendLine("""<network-security-config>""")
                appendLine("""    <base-config cleartextTrafficPermitted="$allowCleartext">""")
                appendLine("""        <trust-anchors>""")
                appendLine("""            <certificates src="system" />""")
                if (trustUserCerts) {
                    appendLine("""            <certificates src="user" />""")
                }
                appendLine("""        </trust-anchors>""")
                appendLine("""    </base-config>""")
                appendLine("""</network-security-config>""")
            }

            val file = get(CONFIG_PATH, copy = false)
            file.parentFile?.mkdirs()
            file.writeText(config)
            logger.info("Wrote $CONFIG_PATH (userCerts=$trustUserCerts, cleartext=$allowCleartext).")
        }

        document("AndroidManifest.xml").use { document ->
            val appNodes = document.getElementsByTagName("application")
            if (appNodes.length > 0) {
                val appElement = appNodes.item(0) as? Element
                if (appElement != null) {
                    if (enableDebugging) {
                        appElement.setAttribute("android:debuggable", "true")
                        logger.info("Set android:debuggable to true")
                    }

                    if (allowCleartext) {
                        appElement.setAttribute("android:usesCleartextTraffic", "true")
                        logger.info("Set android:usesCleartextTraffic to true")
                    }

                    if (removeTestOnly && appElement.hasAttribute("android:testOnly")) {
                        appElement.removeAttribute("android:testOnly")
                        logger.info("Removed android:testOnly attribute")
                    }

                    if (trustUserCerts || allowCleartext) {
                        val previous = appElement.getAttribute("android:networkSecurityConfig")
                        appElement.setAttribute("android:networkSecurityConfig", CONFIG_REF)
                        if (previous.isNotEmpty() && previous != CONFIG_REF) {
                            logger.info("Replaced existing networkSecurityConfig: $previous")
                        }
                    }
                }
            }
        }
    }
}

package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

private val logger = Logger.getLogger("TrustUserCertificatesPatch")

private const val CONFIG_PATH = "res/xml/sqz_network_security_config.xml"
private const val CONFIG_REF = "@xml/sqz_network_security_config"

@Suppress("unused")
val trustUserCertificatesPatch = resourcePatch(
    name = "Trust user certificates",
    description = "Installs a network security config that trusts user-installed CA certificates so HTTPS traffic can be inspected with proxy tools.",
    default = false
) {
    category("Debugging")

    val trustUserCertsOption = booleanOption(
        key = "trustUserCerts",
        default = true,
        title = "Trust User Certificates",
        description = "Accept TLS certificates signed by user-installed certificate authorities (e.g. mitmproxy, HTTP Toolkit).",
    )

    val permitCleartextOption = booleanOption(
        key = "permitCleartext",
        default = true,
        title = "Permit Cleartext Traffic",
        description = "Allow unencrypted HTTP traffic in the generated network security config.",
    )

    execute {
        val trustUserCerts = trustUserCertsOption.value == true
        val permitCleartext = permitCleartextOption.value == true

        if (!trustUserCerts && !permitCleartext) {
            logger.info("Both options disabled, no network security config generated.")
            return@execute
        }

        val config = buildString {
            appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
            appendLine("""<network-security-config>""")
            appendLine("""    <base-config cleartextTrafficPermitted="$permitCleartext">""")
            appendLine("""        <trust-anchors>""")
            appendLine("""            <certificates src="system" />""")
            if (trustUserCerts) {
                appendLine("""            <certificates src="user" />""")
            }
            appendLine("""        </trust-anchors>""")
            appendLine("""    </base-config>""")
            appendLine("""</network-security-config>""")
        }

        // The unique resource name avoids clobbering an app's own network security config file;
        // pointing the manifest at it still supersedes any existing config, which is the intent.
        val file = get(CONFIG_PATH, copy = false)
        file.parentFile?.mkdirs()
        file.writeText(config)

        document("AndroidManifest.xml").use { document ->
            val appNodes = document.getElementsByTagName("application")
            val appElement = appNodes.item(0) as? org.w3c.dom.Element
            if (appElement != null) {
                val previous = appElement.getAttribute("android:networkSecurityConfig")
                appElement.setAttribute("android:networkSecurityConfig", CONFIG_REF)
                if (previous.isNotEmpty() && previous != CONFIG_REF) {
                    logger.info("Replaced existing networkSecurityConfig: $previous")
                }
            }
        }

        logger.info("Wrote $CONFIG_PATH (userCerts=$trustUserCerts, cleartext=$permitCleartext).")
    }
}
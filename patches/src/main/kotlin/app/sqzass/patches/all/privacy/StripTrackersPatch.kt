package app.sqzass.patches.all.privacy

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.PERMISSION_TAGS
import app.sqzass.patches.shared.TrackerPrefixes
import app.sqzass.patches.shared.patchMatchingMethods
import app.sqzass.patches.shared.removeElements
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger
import org.w3c.dom.Element

private val logger = Logger.getLogger("StripTrackersPatch")

private val AD_PERMISSIONS = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
)

// Options live at file scope so the named resource patch registers them for the
// Manager while the unnamed internal bytecode patch reads the same instances.
private val stripGoogleOption = booleanOption(
    key = "stripGoogleAnalytics",
    default = true,
    title = "Strip Google & Firebase Analytics",
    description = "Remove Google Analytics, Firebase Analytics, and AppMeasurement components.",
)

private val stripFacebookOption = booleanOption(
    key = "stripFacebookAnalytics",
    default = true,
    title = "Strip Facebook Analytics",
    description = "Remove Facebook App Events and analytics provider components.",
)

private val stripAttributionOption = booleanOption(
    key = "stripAttributionTrackers",
    default = true,
    title = "Strip Attribution Trackers",
    description = "Remove third-party attribution and tracking SDKs (AppsFlyer, Adjust, Branch, Kochava, Mixpanel, Flurry).",
)

private val stripCrashOption = booleanOption(
    key = "stripCrashReporting",
    default = true,
    title = "Strip Crash Reporting Telemetry",
    description = "Remove Crashlytics and Microsoft App Center telemetry services.",
)

private val stripAdIdOption = booleanOption(
    key = "stripAdvertisingId",
    default = true,
    title = "Strip Advertising ID Permissions",
    description = "Remove AD_ID and AdServices permissions to prevent advertising tracking.",
)

private val disableTrackerCallsOption = booleanOption(
    key = "disableTrackerCalls",
    default = true,
    title = "Disable Tracker SDK Calls",
    description = "Replace void-returning calls into the stripped tracking SDKs with no-ops so they never run.",
)

/** Prefixes of the vendor groups currently enabled for stripping. */
private fun activeTrackerPrefixes(): Set<String> = buildSet {
    if (stripGoogleOption.value == true) addAll(TrackerPrefixes.GOOGLE)
    if (stripFacebookOption.value == true) addAll(TrackerPrefixes.FACEBOOK)
    if (stripAttributionOption.value == true) addAll(TrackerPrefixes.ATTRIBUTION)
    if (stripCrashOption.value == true) addAll(TrackerPrefixes.CRASH)
}

/**
 * Whether this is a void-returning call into an enabled tracking SDK that is safe to nop.
 *
 * Constructors are skipped: noping `<init>` would leave the receiver uninitialized
 * and fail verification. Non-void calls are skipped as well: their result register
 * feeds application logic, so removing them would change control flow instead of
 * just suppressing the tracking side effect. And invoke-super is excluded so super
 * calls into framework base classes are never touched.
 */
private fun isNopableTrackerCall(instruction: Instruction, prefixes: Set<String>): Boolean {
    if (instruction.opcode == Opcode.INVOKE_SUPER) return false
    val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return reference.returnType == "V" &&
        reference.name != "<init>" &&
        TrackerPrefixes.matchesAny(reference.definingClass, prefixes)
}

// Internal patch that is not shown in Morphe Manager or CLI patch list,
// but nops tracker SDK calls as a runtime complement to the manifest stripping.
@Suppress("unused")
private val stripTrackersInternalPatch = bytecodePatch {
    execute {
        if (disableTrackerCallsOption.value != true) {
            logger.info("Tracker SDK call disabling is off.")
            return@execute
        }

        val prefixes = activeTrackerPrefixes()
        if (prefixes.isEmpty()) {
            logger.info("No tracker vendors enabled.")
            return@execute
        }

        val patched = patchMatchingMethods(
            isCandidate = { method ->
                method.implementation?.instructions?.any { isNopableTrackerCall(it, prefixes) } == true
            },
        ) { instructions ->
            var count = 0
            for (index in instructions.indices) {
                if (isNopableTrackerCall(instructions[index], prefixes)) {
                    replaceInstruction(index, "nop")
                    count++
                }
            }
            count
        }

        logger.info("Nopped $patched tracking SDK call sites.")
    }
}

@Suppress("unused")
val stripTrackersPatch = resourcePatch(
    name = "Strip trackers and telemetry",
    description = "Strips known tracking, analytics, and advertising SDK components from the manifest and nops their runtime calls.",
    default = false
) {
    category("Privacy")

    stripGoogleOption()
    stripFacebookOption()
    stripAttributionOption()
    stripCrashOption()
    stripAdIdOption()
    disableTrackerCallsOption()

    dependsOn(stripTrackersInternalPatch)

    execute {
        val activePrefixes = activeTrackerPrefixes()

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

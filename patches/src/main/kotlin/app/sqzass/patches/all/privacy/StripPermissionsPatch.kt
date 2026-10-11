package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.sqzass.patches.shared.PERMISSION_TAGS
import app.sqzass.patches.shared.collectElements
import app.sqzass.patches.shared.removeElements
import java.util.logging.Logger

private val logger = Logger.getLogger("StripPermissionsPatch")

private val CAMERA_PERMISSIONS = setOf(
    "android.permission.CAMERA",
)

private val MICROPHONE_PERMISSIONS = setOf(
    "android.permission.RECORD_AUDIO",
    "android.permission.CAPTURE_AUDIO_OUTPUT",
)

private val LOCATION_PERMISSIONS = setOf(
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_BACKGROUND_LOCATION",
)

private val SENSOR_PERMISSIONS = setOf(
    "android.permission.BODY_SENSORS",
    "android.permission.BODY_SENSORS_BACKGROUND",
    "android.permission.ACTIVITY_RECOGNITION",
)

private val CONTACTS_PERMISSIONS = setOf(
    "android.permission.READ_CONTACTS",
    "android.permission.WRITE_CONTACTS",
    "android.permission.GET_ACCOUNTS",
)

private val PHONE_SMS_PERMISSIONS = setOf(
    "android.permission.READ_PHONE_STATE",
    "android.permission.READ_PHONE_NUMBERS",
    "android.permission.CALL_PHONE",
    "android.permission.ANSWER_PHONE_CALLS",
    "android.permission.READ_CALL_LOG",
    "android.permission.WRITE_CALL_LOG",
    "android.permission.ADD_VOICEMAIL",
    "android.permission.USE_SIP",
    "android.permission.PROCESS_OUTGOING_CALLS",
    "android.permission.READ_SMS",
    "android.permission.RECEIVE_SMS",
    "android.permission.RECEIVE_MMS",
    "android.permission.RECEIVE_WAP_PUSH",
    "android.permission.SEND_SMS",
)

private val CALENDAR_PERMISSIONS = setOf(
    "android.permission.READ_CALENDAR",
    "android.permission.WRITE_CALENDAR",
)

private val STORAGE_PERMISSIONS = setOf(
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_MEDIA_VIDEO",
    "android.permission.READ_MEDIA_AUDIO",
    "android.permission.ACCESS_MEDIA_LOCATION",
)

private val WIFI_PERMISSIONS = setOf(
    "android.permission.NEARBY_WIFI_DEVICES",
    "android.permission.ACCESS_WIFI_STATE",
    "android.permission.CHANGE_WIFI_STATE",
    "android.permission.CHANGE_WIFI_MULTICAST_STATE",
)

private val OVERLAY_PERMISSIONS = setOf(
    "android.permission.SYSTEM_ALERT_WINDOW",
    "android.permission.WRITE_SETTINGS",
)

private val SPECIAL_ACCESS_PERMISSIONS = setOf(
    "android.permission.REQUEST_INSTALL_PACKAGES",
    "android.permission.REQUEST_DELETE_PACKAGES",
    "android.permission.PACKAGE_USAGE_STATS",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
)

private val CAMERA_FEATURES = setOf(
    "android.hardware.camera",
    "android.hardware.camera.autofocus",
    "android.hardware.camera.front",
    "android.hardware.camera.any",
    "android.hardware.camera.flash",
    "android.hardware.camera.level.full",
)

private val MICROPHONE_FEATURES = setOf(
    "android.hardware.microphone",
)

@Suppress("unused")
val stripPermissionsPatch = resourcePatch(
    name = "Strip sensitive permissions",
    description = "Strips sensitive Android permissions (such as camera and microphone access) from the application manifest to enhance privacy.",
    default = false
) {
    category("Privacy")

    val stripCameraOption = booleanOption(
        key = "stripCamera",
        default = true,
        title = "Strip Camera",
        description = "Remove camera access permissions (android.permission.CAMERA).",
    )

    val stripMicrophoneOption = booleanOption(
        key = "stripMicrophone",
        default = true,
        title = "Strip Microphone",
        description = "Remove microphone and audio recording permissions (android.permission.RECORD_AUDIO, etc.).",
    )

    val stripLocationOption = booleanOption(
        key = "stripLocation",
        default = false,
        title = "Strip Location",
        description = "Remove location tracking permissions (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, etc.).",
    )

    val stripSensorsOption = booleanOption(
        key = "stripSensors",
        default = false,
        title = "Strip Sensors",
        description = "Remove body sensor and activity recognition permissions.",
    )

    val stripContactsOption = booleanOption(
        key = "stripContacts",
        default = false,
        title = "Strip Contacts",
        description = "Remove contacts access permissions (READ_CONTACTS, WRITE_CONTACTS, GET_ACCOUNTS).",
    )

    val stripPhoneSmsOption = booleanOption(
        key = "stripPhoneSms",
        default = false,
        title = "Strip Phone & SMS",
        description = "Remove phone, call log, and SMS permissions (READ_PHONE_STATE, READ_CALL_LOG, READ_SMS, SEND_SMS, etc.).",
    )

    val stripCalendarOption = booleanOption(
        key = "stripCalendar",
        default = false,
        title = "Strip Calendar",
        description = "Remove calendar access permissions (READ_CALENDAR, WRITE_CALENDAR).",
    )

    val stripStorageOption = booleanOption(
        key = "stripStorage",
        default = false,
        title = "Strip Storage & Media",
        description = "Remove media and storage permissions (READ_MEDIA_IMAGES, READ_EXTERNAL_STORAGE, etc.).",
    )

    val stripWifiOption = booleanOption(
        key = "stripWifi",
        default = false,
        title = "Strip Nearby Wi-Fi",
        description = "Remove nearby Wi-Fi scanning permissions (NEARBY_WIFI_DEVICES, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE). May affect Wi-Fi status display.",
    )

    val stripOverlayOption = booleanOption(
        key = "stripOverlay",
        default = false,
        title = "Strip Overlay Access",
        description = "Remove permissions that let the app draw over other apps or change system settings (SYSTEM_ALERT_WINDOW, WRITE_SETTINGS).",
    )

    val stripSpecialAccessOption = booleanOption(
        key = "stripSpecialAccess",
        default = false,
        title = "Strip Special Access",
        description = "Remove package installer, usage access, and all-files access permissions (REQUEST_INSTALL_PACKAGES, PACKAGE_USAGE_STATS, MANAGE_EXTERNAL_STORAGE).",
    )

    execute {
        val permissionsToStrip = mutableSetOf<String>()
        val featuresToDowngrade = mutableSetOf<String>()

        if (stripCameraOption.value == true) {
            permissionsToStrip.addAll(CAMERA_PERMISSIONS)
            featuresToDowngrade.addAll(CAMERA_FEATURES)
        }

        if (stripMicrophoneOption.value == true) {
            permissionsToStrip.addAll(MICROPHONE_PERMISSIONS)
            featuresToDowngrade.addAll(MICROPHONE_FEATURES)
        }

        if (stripLocationOption.value == true) {
            permissionsToStrip.addAll(LOCATION_PERMISSIONS)
        }

        if (stripSensorsOption.value == true) {
            permissionsToStrip.addAll(SENSOR_PERMISSIONS)
        }

        if (stripContactsOption.value == true) {
            permissionsToStrip.addAll(CONTACTS_PERMISSIONS)
        }

        if (stripPhoneSmsOption.value == true) {
            permissionsToStrip.addAll(PHONE_SMS_PERMISSIONS)
        }

        if (stripCalendarOption.value == true) {
            permissionsToStrip.addAll(CALENDAR_PERMISSIONS)
        }

        if (stripStorageOption.value == true) {
            permissionsToStrip.addAll(STORAGE_PERMISSIONS)
        }

        if (stripWifiOption.value == true) {
            permissionsToStrip.addAll(WIFI_PERMISSIONS)
        }

        if (stripOverlayOption.value == true) {
            permissionsToStrip.addAll(OVERLAY_PERMISSIONS)
        }

        if (stripSpecialAccessOption.value == true) {
            permissionsToStrip.addAll(SPECIAL_ACCESS_PERMISSIONS)
        }

        if (permissionsToStrip.isEmpty()) {
            logger.info("No permissions selected to strip.")
            return@execute
        }

        document("AndroidManifest.xml").use { document ->
            val removedPermissions = removeElements(document, PERMISSION_TAGS) { element ->
                permissionsToStrip.contains(element.getAttribute("android:name"))
            }

            // Adjust hardware features if their corresponding permissions were stripped.
            val features = collectElements(document, listOf("uses-feature")) { element ->
                featuresToDowngrade.contains(element.getAttribute("android:name"))
            }
            for (element in features) {
                // Mark as not required so app does not refuse to run on devices without the hardware
                element.setAttribute("android:required", "false")
                logger.info("Set feature ${element.getAttribute("android:name")} android:required to false")
            }

            logger.info("Successfully stripped permissions: $removedPermissions")
        }
    }
}

package app.sqzass.patches.all.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
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

        if (permissionsToStrip.isEmpty()) {
            logger.info("No permissions selected to strip.")
            return@execute
        }

        document("AndroidManifest.xml").use { document ->
            val permissionTags = listOf("uses-permission", "uses-permission-sdk-23")
            val removedPermissions = mutableListOf<String>()

            for (tag in permissionTags) {
                val nodes = document.getElementsByTagName(tag)
                val elementsToRemove = mutableListOf<Element>()

                for (i in 0 until nodes.length) {
                    val element = nodes.item(i) as? Element ?: continue
                    val name = element.getAttribute("android:name")
                    if (permissionsToStrip.contains(name)) {
                        elementsToRemove.add(element)
                        removedPermissions.add(name)
                    }
                }

                for (element in elementsToRemove) {
                    element.parentNode?.removeChild(element)
                }
            }

            // Adjust hardware features if their corresponding permissions were stripped
            if (featuresToDowngrade.isNotEmpty()) {
                val featureNodes = document.getElementsByTagName("uses-feature")
                for (i in 0 until featureNodes.length) {
                    val element = featureNodes.item(i) as? Element ?: continue
                    val featureName = element.getAttribute("android:name")
                    if (featuresToDowngrade.contains(featureName)) {
                        // Mark as not required so app does not refuse to run on devices without the hardware
                        element.setAttribute("android:required", "false")
                        logger.info("Set feature $featureName android:required to false")
                    }
                }
            }

            logger.info("Successfully stripped permissions: $removedPermissions")
        }
    }
}

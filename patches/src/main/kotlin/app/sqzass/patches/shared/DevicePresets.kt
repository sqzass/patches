package app.sqzass.patches.shared

/**
 * Build fingerprint fields spoofed for a device preset.
 *
 * [imei] is a dummy value used only when device ID spoofing is enabled.
 */
data class DevicePreset(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val hardware: String,
    val display: String,
    val fingerprint: String,
    val imei: String,
)

/** SIM/carrier identity fields spoofed for a carrier preset. */
data class SimPreset(
    val carrier: String,
    val mcc: String,
    val mnc: String,
    val countryIso: String,
    val iccid: String,
    val imsi: String,
) {
    /** MCC+MNC as reported by getSimOperator/getNetworkOperator. */
    val operatorCode: String get() = mcc + mnc
}

// ponytail: presets carry representative stock-style values chosen so internal
// cross-checks pass (manufacturer appears in the fingerprint, model matches the
// device tree); they are not certified copies of specific factory builds.
object DevicePresets {
    val DEVICES: Map<String, DevicePreset> = linkedMapOf(
        "samsung-galaxy-s24" to DevicePreset(
            manufacturer = "samsung",
            brand = "samsung",
            model = "SM-S921B",
            device = "dm3q",
            product = "dm3q",
            hardware = "qcom",
            display = "UP1A.231005.007",
            fingerprint = "samsung/dm3q/dm3q:14/UP1A.231005.007/S921BXXU1AWM9:user/release-keys",
            imei = "352099001234561",
        ),
        "samsung-galaxy-a54" to DevicePreset(
            manufacturer = "samsung",
            brand = "samsung",
            model = "SM-A546B",
            device = "a54x",
            product = "a54x",
            hardware = "s5e9945",
            display = "UP1A.230805.001",
            fingerprint = "samsung/a54x/a54x:14/UP1A.230805.001/S946BXXU5CWL1:user/release-keys",
            imei = "352100001234567",
        ),
        "google-pixel-8" to DevicePreset(
            manufacturer = "Google",
            brand = "google",
            model = "Pixel 8",
            device = "husky",
            product = "husky",
            hardware = "husky",
            display = "UQ1A.240205.004",
            fingerprint = "google/husky/husky:14/UQ1A.240205.004/11220150:user/release-keys",
            imei = "353456789012345",
        ),
        "google-pixel-7" to DevicePreset(
            manufacturer = "Google",
            brand = "google",
            model = "Pixel 7",
            device = "panther",
            product = "panther",
            hardware = "panther",
            display = "UQ1A.240205.004",
            fingerprint = "google/panther/panther:14/UQ1A.240205.004/11220150:user/release-keys",
            imei = "353111112222333",
        ),
        "google-pixel-6a" to DevicePreset(
            manufacturer = "Google",
            brand = "google",
            model = "Pixel 6a",
            device = "bluejay",
            product = "bluejay",
            hardware = "bluejay",
            display = "UQ1A.240205.004",
            fingerprint = "google/bluejay/bluejay:14/UQ1A.240205.004/11220150:user/release-keys",
            imei = "353222223333444",
        ),
        "xiaomi-13" to DevicePreset(
            manufacturer = "Xiaomi",
            brand = "Xiaomi",
            model = "2211133G",
            device = "fuxi",
            product = "fuxi",
            hardware = "qcom",
            display = "TKQ1.221114.001",
            fingerprint = "Xiaomi/fuxi/fuxi:13/TKQ1.221114.001/V14.0.23.0.TMFCNXM:user/release-keys",
            imei = "869000001111222",
        ),
        "oneplus-11" to DevicePreset(
            manufacturer = "OnePlus",
            brand = "OnePlus",
            model = "CPH2449",
            device = "salami",
            product = "salami",
            hardware = "qcom",
            display = "TP1A.220905.001",
            fingerprint = "OnePlus/CPH2449/OP594B1:13/TP1A.220905.001/R.202301011:user/release-keys",
            imei = "861000002222333",
        ),
        "motorola-edge-40" to DevicePreset(
            manufacturer = "motorola",
            brand = "motorola",
            model = "Edge 40",
            device = "lyriq",
            product = "lyriq_g",
            hardware = "qcom",
            display = "T1TS33.60-15",
            fingerprint = "motorola/lyriq_g/lyriq:13/T1TS33.60-15/880574:user/release-keys",
            imei = "354000003333444",
        ),
    )

    val SIMS: Map<String, SimPreset> = linkedMapOf(
        "tmobile-us" to SimPreset(
            carrier = "T-Mobile",
            mcc = "310",
            mnc = "260",
            countryIso = "us",
            iccid = "3102601234567890123",
            imsi = "310260123456789",
        ),
        "verizon-us" to SimPreset(
            carrier = "Verizon",
            mcc = "311",
            mnc = "480",
            countryIso = "us",
            iccid = "3114801234567890123",
            imsi = "311480123456789",
        ),
        "att-us" to SimPreset(
            carrier = "AT&T",
            mcc = "310",
            mnc = "410",
            countryIso = "us",
            iccid = "3104101234567890123",
            imsi = "310410123456789",
        ),
        "vodafone-de" to SimPreset(
            carrier = "Vodafone",
            mcc = "262",
            mnc = "02",
            countryIso = "de",
            iccid = "2620201234567890123",
            imsi = "262020123456789",
        ),
        "orange-fr" to SimPreset(
            carrier = "Orange",
            mcc = "208",
            mnc = "01",
            countryIso = "fr",
            iccid = "2080101234567890123",
            imsi = "208010123456789",
        ),
        "o2-gb" to SimPreset(
            carrier = "O2",
            mcc = "234",
            mnc = "10",
            countryIso = "gb",
            iccid = "2341001234567890123",
            imsi = "234100123456789",
        ),
        "airtel-in" to SimPreset(
            carrier = "Airtel",
            mcc = "404",
            mnc = "10",
            countryIso = "in",
            iccid = "4041001234567890123",
            imsi = "404100123456789",
        ),
        "jio-in" to SimPreset(
            carrier = "Jio",
            mcc = "405",
            mnc = "872",
            countryIso = "in",
            iccid = "4058701234567890123",
            imsi = "405872123456789",
        ),
        "movistar-es" to SimPreset(
            carrier = "Movistar",
            mcc = "214",
            mnc = "07",
            countryIso = "es",
            iccid = "2140701234567890123",
            imsi = "214070123456789",
        ),
    )

    /** Preset key → display name, for the device preset option dropdown. */
    val DEVICE_LABELS: Map<String, String> = linkedMapOf(
        "samsung-galaxy-s24" to "Samsung Galaxy S24",
        "samsung-galaxy-a54" to "Samsung Galaxy A54",
        "google-pixel-8" to "Google Pixel 8",
        "google-pixel-7" to "Google Pixel 7",
        "google-pixel-6a" to "Google Pixel 6a",
        "xiaomi-13" to "Xiaomi 13",
        "oneplus-11" to "OnePlus 11",
        "motorola-edge-40" to "Motorola Edge 40",
    )

    /** Preset key → display name, for the SIM preset option dropdown. */
    val SIM_LABELS: Map<String, String> = linkedMapOf(
        "tmobile-us" to "T-Mobile (US)",
        "verizon-us" to "Verizon (US)",
        "att-us" to "AT&T (US)",
        "vodafone-de" to "Vodafone (DE)",
        "orange-fr" to "Orange (FR)",
        "o2-gb" to "O2 (UK)",
        "airtel-in" to "Airtel (IN)",
        "jio-in" to "Jio (IN)",
        "movistar-es" to "Movistar (ES)",
    )
}
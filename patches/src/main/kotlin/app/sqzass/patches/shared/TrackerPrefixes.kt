package app.sqzass.patches.shared

/**
 * Class-name prefixes of the tracking SDKs targeted by the tracker patches.
 * Used both for manifest component names (dotted) and bytecode method references
 * (type descriptors), normalized by [matches].
 */
object TrackerPrefixes {
    val GOOGLE = setOf(
        "com.google.android.gms.measurement.",
        "com.google.firebase.analytics.",
        "com.google.android.gms.analytics.",
        "com.google.android.gms.ads.",
    )

    val FACEBOOK = setOf(
        "com.facebook.appevents.",
        "com.facebook.internal.FacebookInitProvider",
        "com.facebook.marketing.",
    )

    val ATTRIBUTION = setOf(
        "com.appsflyer.",
        "com.adjust.sdk.",
        "io.branch.",
        "com.kochava.",
        "com.mixpanel.",
        "com.flurry.",
    )

    val CRASH = setOf(
        "com.google.firebase.crashlytics.",
        "com.crashlytics.",
        "com.microsoft.appcenter.",
    )

    /** Every tracked prefix, for patches that do not offer per-vendor options. */
    val ALL = GOOGLE + FACEBOOK + ATTRIBUTION + CRASH

    /**
     * Whether a class type matches any tracked prefix. Accepts both dotted class names
     * and type descriptors such as `Lcom/appsflyer/AFInAppEventParameterName;`.
     */
    fun matches(classType: String): Boolean = matchesAny(classType, ALL)

    /**
     * Whether a class type matches any of [prefixes], normalizing dotted class names
     * and type descriptors alike.
     */
    fun matchesAny(classType: String, prefixes: Set<String>): Boolean {
        val dotted = classType.removePrefix("L").removeSuffix(";").replace('/', '.')
        return prefixes.any { prefix -> dotted.startsWith(prefix) }
    }
}
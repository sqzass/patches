plugins {
    id("checkstyle")
}

extension {
    name = "extensions/extension.mpe"
}

android {
    // TODO: Update this namespace to match your actual package name! 
    // (e.g., "app.sqzass.patches.extension" or whatever your base package is)
    namespace = "app.sqzass.patches.extension"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "BUNDLE_VERSION", "\"${project.version}\"")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("user.timezone", "UTC")
                it.maxHeapSize = "2g"
                it.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.net=ALL-UNNAMED",
                    "--add-opens=java.base/java.security=ALL-UNNAMED",
                    "--add-opens=java.base/java.text=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                )
            }
        }
    }
}

// Optional: Configure checkstyle if you have a specific config file. 
// Otherwise, the default plugin behavior will just check src/main/java and src/test/java.
tasks.withType<Checkstyle>().configureEach {
    // Remove the moviebox/ijiami paths. Point only to your actual source directories.
    source("src/main/java", "src/test/java")
    classpath = files()
}

dependencies {
    checkstyle(libs.spring.javaformat.checkstyle)
    implementation(libs.morphe.extensions)
    compileOnly(libs.okhttp)
    compileOnly(libs.androidx.annotation)
    compileOnly(libs.androidx.compose.ui)
    compileOnly(libs.androidx.compose.runtime)
    compileOnly(libs.androidx.compose.foundation)
    compileOnly(libs.androidx.webkit)
    
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp)
    testImplementation(libs.json)
}plugins {
    id("checkstyle")
}

extension {
    name = "extensions/extension.mpe"
}

android {
    // TODO: Update this namespace to match your actual package name! 
    // (e.g., "app.sqzass.patches.extension" or whatever your base package is)
    namespace = "app.sqzass.patches.extension"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "BUNDLE_VERSION", "\"${project.version}\"")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("user.timezone", "UTC")
                it.maxHeapSize = "2g"
                it.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.net=ALL-UNNAMED",
                    "--add-opens=java.base/java.security=ALL-UNNAMED",
                    "--add-opens=java.base/java.text=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                )
            }
        }
    }
}

// Optional: Configure checkstyle if you have a specific config file. 
// Otherwise, the default plugin behavior will just check src/main/java and src/test/java.
tasks.withType<Checkstyle>().configureEach {
    // Remove the moviebox/ijiami paths. Point only to your actual source directories.
    source("src/main/java", "src/test/java")
    classpath = files()
}

dependencies {
    checkstyle(libs.spring.javaformat.checkstyle)
    implementation(libs.morphe.extensions)
    compileOnly(libs.okhttp)
    compileOnly(libs.androidx.annotation)
    compileOnly(libs.androidx.compose.ui)
    compileOnly(libs.androidx.compose.runtime)
    compileOnly(libs.androidx.compose.foundation)
    compileOnly(libs.androidx.webkit)
    
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp)
    testImplementation(libs.json)
}
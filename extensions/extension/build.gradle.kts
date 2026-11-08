extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.hxreborn.extension"

    sourceSets.getByName("test").java.srcDirs("../moviebox/src", "../moviebox/test", "../ijiami/src", "../ijiami/test")

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
                    "--add-opens=java-base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                )
            }
        }
    }
}

tasks.register<Checkstyle>("checkstyle") {
    source("src/main/java", "../moviebox/src", "../moviebox/test", "../ijiami/test")
    classpath = files()
}

val movieBoxMinSdk = 23

val checkMovieBoxApiLevel = tasks.register<Exec>("checkMovieBoxApiLevel") {
    dependsOn("compileDebugUnitTestJavaWithJavac")
    val apiVersions = androidComponents.sdkComponents.sdkDirectory.map {
        it.file("platforms/android-${android.compileSdk}/data/api-versions.xml").asFile.path
    }
    val classes = layout.buildDirectory.dir("intermediates/javac/debugUnitTest/compileDebugUnitTestJavaWithJavac/classes")
    val sources = layout.projectDirectory.dir("../moviebox/src")
    inputs.dir(classes)
    inputs.dir(sources)
    commandLine(
        "python3",
        rootProject.file(".github/scripts/check_android_api_level.py").path,
        movieBoxMinSdk.toString(),
        apiVersions.get(),
        classes.get().asFile.path,
        sources.asFile.path,
    )
}

tasks.matching { it.name == "lintRelease" }.configureEach {
    dependsOn(checkMovieBoxApiLevel)
}

tasks.named("check") {
    dependsOn("checkstyle")
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

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun prop(key: String, default: String): String =
    (project.findProperty(key) as? String)
        ?: localProps.getProperty(key)
        ?: default

android {
    namespace = "stufforg.capstone.frontend"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "stufforg.capstone.frontend"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiBaseUrl = prop("API_BASE_URL", "http://10.0.2.2:8000/api/")
            .let { if (it.endsWith("/")) it else "$it/" }
        val googleWebClientId = prop("GOOGLE_WEB_CLIENT_ID", "")

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.print)
    implementation(libs.bundles.network)
    implementation(libs.bundles.coil)
    implementation(libs.bundles.google.auth)
    implementation(libs.play.services.code.scanner)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register<Exec>("reverseAdb") {
    group = "running"
    description = "Forward device port 8000 to host port 8000 over USB"
    val sdkDir = localProps.getProperty("sdk.dir")
        ?: System.getenv("ANDROID_HOME")
        ?: "${System.getProperty("user.home")}/Library/Android/sdk"
    val adbExecutable = file("$sdkDir/platform-tools/adb")
    commandLine(adbExecutable.absolutePath, "reverse", "tcp:8000", "tcp:8000")
    isIgnoreExitValue = true
}

tasks.matching { it.name.startsWith("install") || it.name.startsWith("assemble") }.configureEach {
    finalizedBy("reverseAdb")
}

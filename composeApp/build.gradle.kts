import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

fun signingValue(name: String): String? =
    keystoreProperties.getProperty(name) ?: System.getenv("ANDROID_${name.uppercase()}")

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:1.10.3")
            implementation("org.jetbrains.compose.foundation:foundation:1.10.3")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.components:components-resources:1.10.3")
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.10.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")
            implementation("dev.gitlive:firebase-auth:2.4.0")
            implementation("dev.gitlive:firebase-firestore:2.4.0")
            implementation("org.maplibre.compose:maplibre-compose:0.12.1")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("com.russhwolf:multiplatform-settings-test:1.3.0")
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.13.0")
        }
    }
}

android {
    namespace = "de.zugspitz.supporter"
    compileSdk = 36

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "de.zugspitz.supporter"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    signingConfigs {
        create("release") {
            val storeFilePath = signingValue("storeFile")
            if (!storeFilePath.isNullOrBlank()) {
                storeFile = rootProject.file(storeFilePath)
            }
            storePassword = signingValue("storePassword")
            keyAlias = signingValue("keyAlias")
            keyPassword = signingValue("keyPassword")
        }
    }
    buildTypes {
        getByName("debug") {
            buildConfigField("boolean", "LIVE_SHARING_ENABLED", "true")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("boolean", "LIVE_SHARING_ENABLED", "false")
        }
    }
}

dependencies {
    add("androidMainImplementation", platform("com.google.firebase:firebase-bom:34.13.0"))
    add("androidMainImplementation", "com.google.firebase:firebase-auth")
    add("androidMainImplementation", "com.google.firebase:firebase-firestore")
    debugImplementation("org.jetbrains.compose.ui:ui-tooling:1.10.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

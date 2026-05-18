import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.application")
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

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun localConfigValue(name: String): String? =
    localProperties.getProperty(name) ?: System.getenv(name)

fun escapedBuildConfigString(value: String?): String =
    "\"${(value ?: "").replace("\\", "\\\\").replace("\"", "\\\"")}\""

val supabaseUrl = localConfigValue("SUPABASE_URL")
val supabasePublishableKey = localConfigValue("SUPABASE_PUBLISHABLE_KEY")
    ?: localConfigValue("SUPABASE_ANON_KEY")

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
            implementation(project.dependencies.platform("io.github.jan-tennert.supabase:bom:3.2.4"))
            implementation("io.github.jan-tennert.supabase:auth-kt")
            implementation("io.github.jan-tennert.supabase:postgrest-kt")
            implementation("io.github.jan-tennert.supabase:realtime-kt")
            implementation("org.maplibre.compose:maplibre-compose:0.12.1")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("com.russhwolf:multiplatform-settings-test:1.3.0")
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.13.0")
            implementation("io.ktor:ktor-client-okhttp:3.3.1")
        }

        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.3.1")
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "de.zugspitz.supporter"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.3"
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
            buildConfigField("String", "SUPABASE_URL", escapedBuildConfigString(supabaseUrl))
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", escapedBuildConfigString(supabasePublishableKey))
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("boolean", "LIVE_SHARING_ENABLED", "true")
            buildConfigField("String", "SUPABASE_URL", escapedBuildConfigString(supabaseUrl))
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", escapedBuildConfigString(supabasePublishableKey))
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    debugImplementation("org.jetbrains.compose.ui:ui-tooling:1.10.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

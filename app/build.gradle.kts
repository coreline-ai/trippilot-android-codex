plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "io.trippilot.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.trippilot.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "ALLOW_REAL_CODEX_OAUTH", "false")
        }
        create("secureDebug") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".securedebug"
            versionNameSuffix = "-secure-debug"
            isDebuggable = false
            matchingFallbacks += listOf("debug")
            ndk { abiFilters += "arm64-v8a" }
            buildConfigField("boolean", "ALLOW_REAL_CODEX_OAUTH", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "ALLOW_REAL_CODEX_OAUTH", "true")
            ndk { abiFilters += "arm64-v8a" }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    // GPL-3.0 vendor source, pinned in third_party/alpine-codex-cli-client. The public
    // TripPilot boundary remains CodexRuntimePort; no runtime type enters feature/UI code.
    implementation(project(":alpine-runtime-api"))
    implementation(project(":alpine-runtime-android"))
    implementation(project(":alpine-runtime-host"))
    implementation(project(":alpine-runtime-pack-bundled"))
    implementation(project(":alpine-python-pack-bundled"))
    implementation(project(":codex-cli-pack"))
    implementation(project(":codex-gateway-pack-bundled"))
    implementation(project(":codex-runtime-bridge"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    implementation(libs.androidx.profileinstaller)
    kapt(libs.hilt.compiler)
    kapt(libs.androidx.room.compiler)

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("androidx.room:room-testing:2.7.2")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    baselineProfile(project(":baselineprofile"))
}

baselineProfile {
    // Generated only by the explicit release-performance command, never by a normal local build.
    automaticGenerationDuringBuild = false
    saveInSrc = true
    mergeIntoMain = true
    filter {
        include("io.trippilot.app.**")
    }
}

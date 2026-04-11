import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localPropsFile = rootProject.file("local.properties")
val localProps = Properties().also { if (localPropsFile.exists()) it.load(localPropsFile.inputStream()) }

android {
    namespace = "io.cadence.music"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.cadence.music"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.2.0"

        buildConfigField("String", "SONGGEN_BASE_URL", "\"${localProps.getProperty("songgen.base.url", "http://82.10.52.198:8000")}\"")
        buildConfigField("String", "GEMMA_BASE_URL", "\"${localProps.getProperty("gemma.base.url", "http://82.10.52.198:8001")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProps.getProperty("gemini.api.key", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Media / ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // Location
    implementation(libs.play.services.location)

    // Health Connect
    implementation(libs.health.connect)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)
}

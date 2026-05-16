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
val releaseKeystorePath = localProps.getProperty("release.keystore.path")
val releaseKeystorePassword = localProps.getProperty("release.keystore.password")
val releaseKeyAlias = localProps.getProperty("release.key.alias")
val releaseKeyPassword = localProps.getProperty("release.key.password")
val releaseSigningProps = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val hasAllReleaseSigningProps = releaseSigningProps.all { !it.isNullOrBlank() }
val releaseKeystoreFile = releaseKeystorePath?.let { rootProject.file(it) }
val releaseSigningEnabled = hasAllReleaseSigningProps && releaseKeystoreFile?.isFile == true

android {
    namespace = "io.cadence.music"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.cadence.music"
        minSdk = 30
        targetSdk = 36
        versionCode = 10
        versionName = "0.6.0"

        buildConfigField("String", "SIGNAL2STYLE_BASE_URL", "\"${localProps.getProperty("signal2style.base.url", "https://openrouter.ai/api/v1")}\"")
        buildConfigField("String", "SIGNAL2STYLE_API_KEY", "\"${localProps.getProperty("signal2style.api.key", "")}\"")
        buildConfigField("String", "SIGNAL2STYLE_MODEL", "\"${localProps.getProperty("signal2style.model", "openrouter/free")}\"")
        buildConfigField("String", "SONGGEN_BASE_URL", "\"${localProps.getProperty("songgen.base.url", "https://api.cadencemusics.uk/v1/music_generation")}\"")
        buildConfigField("String", "SONGGEN_API_KEY", "\"${localProps.getProperty("songgen.api.key", "")}\"")
        buildConfigField("String", "SONGGEN_MODEL", "\"${localProps.getProperty("songgen.model", "SongGeneration-v2-large")}\"")
    }

    signingConfigs {
        if (releaseSigningEnabled) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseKeystorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseSigningEnabled) {
                signingConfig = signingConfigs.getByName("release")
            }
            ndk {
                debugSymbolLevel = "FULL"
            }
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

    testOptions {
        unitTests.isReturnDefaultValues = true
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
    implementation(libs.androidx.ui.text.google.fonts)
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

    // Taste memory persistence
    implementation(libs.datastore.preferences)

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

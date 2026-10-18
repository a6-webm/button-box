import java.nio.file.Files
import java.net.URI
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.buttonbox"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.buttonbox"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
}

fun urlFile(url: String, name: String): String {
    val path = Paths.get("${layout.buildDirectory.get()}/download/${name}")
    println(path)
    Files.createDirectories(path.parent)
    if (!path.exists()) {
        Files.copy(URI(url).toURL().openStream(), path, StandardCopyOption.REPLACE_EXISTING)
    }
    return path.toString()
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(files(urlFile("https://github.com/billthefarmer/mididriver/releases/download/v1.29/MidiDriver-1.29.aar", "MidiDriver-1.29.aar")))
}
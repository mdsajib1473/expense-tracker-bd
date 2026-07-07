plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.sajib.smsexpensetracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sajib.smsexpensetracker"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // These three checks are time-relative: they hit the network and warn
        // whenever any newer release exists, so a build that is clean today
        // reports warnings next month with no code change. We pin deliberate
        // versions in gradle/libs.versions.toml and treat "a newer version
        // exists" as informational, not a build failure. Every other lint
        // check stays fully enabled.
        disable += setOf(
            "NewerVersionAvailable",
            "GradleDependency",
            "AndroidGradlePluginVersion"
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose. The BOM pins every androidx.compose artifact to a tested set.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Dependency injection.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Local database.
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Async.
    implementation(libs.coroutines.android)

    // Unit testing.
    testImplementation(libs.junit)
}

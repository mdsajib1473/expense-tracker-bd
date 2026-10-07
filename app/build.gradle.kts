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

    // Play Store compliance (AGENT.md rule 11). "play" declares no SMS
    // permission, no SMS receiver and no INTERNET permission; "full" keeps
    // live SMS capture via src/full and installs side by side with play.
    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
        }
        create("full") {
            dimension = "distribution"
            applicationIdSuffix = ".full"
        }
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

    testOptions {
        unitTests {
            // Robolectric needs the merged Android manifest and resources to
            // build its simulated application context.
            isIncludeAndroidResources = true
        }
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

// AppDatabase sets exportSchema = true, so Room writes each schema version's
// JSON here. These files are committed and become the ground truth that
// future explicit migrations are validated against (AGENT.md Hard Constraint 5).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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
    implementation(libs.hilt.navigation.compose)

    // Local database.
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Async.
    implementation(libs.coroutines.android)

    // Unit testing. Robolectric provides the simulated Android runtime that
    // the Room DAO tests run against without a device.
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.coroutines.test)
    // XmlPullParser for the backup reader tests on the plain JVM, where
    // android.util.Xml is only a stub. Test classpath only.
    testImplementation(libs.kxml2)
}

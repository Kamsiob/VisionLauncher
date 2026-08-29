import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Semantic version. versionCode is major * 10000 + minor * 100 + patch.
//
// 0.1.0: the foundation and Stage 1, the spine. The first version number because
// this is the first buildable state of the app; 1.0.0 is reserved for the Play
// release once all four stages are verified on a real device.
val versionMajor = 0
val versionMinor = 1
val versionPatch = 0

// One application ID, no debug suffix. The user keeps exactly one copy of this
// app on any device, so debug and release deliberately collide rather than
// coexist. See DECISIONS.md D22.
val launcherApplicationId = "io.github.kamsiob.launcher"

android {
    namespace = "io.github.kamsiob.launcher"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = launcherApplicationId
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schemas are committed so migrations can be written against a
        // known shape when messaging persistence arrives in Stage 2.
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
    }
}

// The em dash gate runs before any assemble so banned punctuation never
// reaches a build.
tasks.matching { it.name.startsWith("assemble") || it.name.startsWith("bundle") }
    .configureEach { dependsOn(rootProject.tasks.named("emDashGate")) }

/**
 * The zero network promise, enforced rather than trusted.
 *
 * The app holds no INTERNET permission, so Android refuses any connection it
 * could attempt, and a person can verify that themselves in the app info
 * screen. A library could introduce the permission through manifest merging
 * without anyone noticing, so the merged manifest is checked on every build.
 *
 * If the opt-in weather glance ever ships, this check is what has to be
 * changed deliberately, in the same commit that adds the feature and the
 * README sentence describing it.
 */
androidComponents {
    onVariants { variant ->
        val manifest = variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.MERGED_MANIFEST)
        val check = tasks.register("check${variant.name.replaceFirstChar { it.uppercase() }}HasNoInternet") {
            inputs.file(manifest)
            doLast {
                val text = manifest.get().asFile.readText()
                if (text.contains("android.permission.INTERNET")) {
                    throw GradleException(
                        "INTERNET permission reached the merged manifest. The app promises " +
                            "that everything happens on the device, so this has to be a " +
                            "deliberate change, not a merge."
                    )
                }
            }
        }
        // Both lifecycle tasks. Play receives an AAB from bundleRelease, so
        // wiring only assemble left the one artifact that actually ships as the
        // one build the gate never checked. The em dash gate above already
        // covers both, which is what made this an oversight rather than a
        // decision.
        val cap = variant.name.replaceFirstChar { c -> c.uppercase() }
        tasks.matching { it.name == "assemble$cap" || it.name == "bundle$cap" }
            .configureEach { dependsOn(check) }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

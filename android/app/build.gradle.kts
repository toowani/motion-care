import java.util.Properties

plugins {
    id("com.android.application")
}

// Pleos Playground SDK app keys (TTS/STT). Kept in local.properties, which is not committed.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun localProp(key: String) = "\"" + (localProps.getProperty(key) ?: "") + "\""

android {
    namespace = "ai.motioncare.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "ai.motioncare.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "PLEOS_CLIENT_ID", localProp("pleos.clientId"))
        buildConfigField("String", "PLEOS_CLIENT_SECRET", localProp("pleos.clientSecret"))
    }

    buildFeatures {
        buildConfig = true
    }

    // release signing: keystore path and passwords live in local.properties (not committed)
    signingConfigs {
        create("release") {
            localProps.getProperty("release.storeFile")?.let { storeFile = rootProject.file(it) }
            storePassword = localProps.getProperty("release.storePassword")
            keyAlias = localProps.getProperty("release.keyAlias")
            keyPassword = localProps.getProperty("release.keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (localProps.getProperty("release.storeFile") != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // CarPropertyManager (PERF_VEHICLE_SPEED) lives in the platform's android.car library
    useLibrary("android.car")
}

// Scenario CSVs and params come from the simulator (STEP 3–4) so replay mode uses the same data as the tests.
val syncSimAssets = tasks.register<Copy>("syncSimAssets") {
    from(rootDir.resolve("../sim/scenarios")) { include("*.csv", "*.route.json") }
    from(rootDir.resolve("../shared/params.json"))
    into(layout.projectDirectory.dir("src/main/assets"))
}
tasks.named("preBuild") { dependsOn(syncSimAssets) }

dependencies {
    implementation(project(":core"))
    // PathParser: themes keep character outlines as SVG path data
    implementation("androidx.core:core:1.13.1")
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
}

// contest naming: apk/팀명_앱이름_release.apk (set apk.teamName in local.properties)
val teamName = localProps.getProperty("apk.teamName") ?: "Team"
val copyReleaseApk = tasks.register<Copy>("copyReleaseApk") {
    inputs.property("teamName", teamName)   // a new team name must re-run the copy
    from(layout.buildDirectory.dir("outputs/apk/release")) { include("*.apk") }
    into(rootDir.resolve("../apk"))
    rename { "${teamName}_MotionCare_release.apk" }
}
tasks.matching { it.name == "assembleRelease" }.configureEach { finalizedBy(copyReleaseApk) }

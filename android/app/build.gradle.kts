plugins {
    id("com.android.application")
}

android {
    namespace = "ai.motioncare.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "ai.motioncare.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
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
    from(rootDir.resolve("../sim/scenarios")) { include("*.csv") }
    from(rootDir.resolve("../shared/params.json"))
    into(layout.projectDirectory.dir("src/main/assets"))
}
tasks.named("preBuild") { dependsOn(syncSimAssets) }

dependencies {
    implementation(project(":core"))
    implementation("ai.pleos.playground:Vehicle:2.3.2")
}

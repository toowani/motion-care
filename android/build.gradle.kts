plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.20" apply false
}

// The project lives in an iCloud-synced folder, which copies build files mid-build ("Foo 2.class")
// and breaks dex merging. iCloud skips folders ending in .nosync.
allprojects {
    layout.buildDirectory.set(file("build.nosync"))
}

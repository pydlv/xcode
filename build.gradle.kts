plugins {
    kotlin("multiplatform") version "1.9.23" // Ensure this is a recent KMP plugin version
    kotlin("plugin.serialization") version "1.9.23"
}

group = "org.giraffemail.xcode"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google() // For Compose, if you add it later
}

kotlin {
    linuxX64("nativeLinux") {
        binaries.executable {
            entryPoint = "org.giraffemail.xcode.main"
        }
    }
    macosX64("nativeMacos") { // For Intel Macs
        binaries.executable {
            entryPoint = "org.giraffemail.xcode.main"
        }
    }
    // macosArm64("nativeMacosArm") { // For Apple Silicon Macs, uncomment if needed
    //     binaries.executable {
    //         entryPoint = "org.giraffemail.xcode.main"
    //     }
    // }
    mingwX64("nativeWindows") {
        binaries.executable {
            entryPoint = "org.giraffemail.xcode.main"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val nativeLinuxMain by getting {
            dependsOn(commonMain)
        }
        val nativeLinuxTest by getting {
            dependsOn(commonTest)
        }

        val nativeMacosMain by getting {
            dependsOn(commonMain)
        }
        val nativeMacosTest by getting {
            dependsOn(commonTest)
        }

        val nativeWindowsMain by getting {
            dependsOn(commonMain)
        }
        val nativeWindowsTest by getting {
            dependsOn(commonTest)
        }
    }
}

// Optional: Configure run tasks for native targets if not automatically created as desired
// tasks.register<Exec>("runNativeLinux") {
//     group = "application"
//     description = "Runs the native Linux executable."
//     dependsOn("linkReleaseExecutableNativeLinux") // or linkDebugExecutableNativeLinux
//     commandLine(kotlin.targets.getByName<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>("nativeLinux").binaries.getExecutable().outputFile.absolutePath)
// }
// Similar tasks can be registered for nativeMacos and nativeWindows if needed.
// Often, tasks like `runDebugExecutableNativeLinux` are already available.

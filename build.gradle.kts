import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask

plugins {
    kotlin("multiplatform") version "2.1.21" // Ensure this is a recent KMP plugin version
    id("com.strumenta.antlr-kotlin") version "1.0.5"
}

val antlrKotlinVersion by extra("1.0.5")

group = "org.giraffemail.xcode"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google() // For Compose, if you add it later
}

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")

    // ANTLR .g4 files are under {example-project}/antlr
    // Only include *.g4 files. This allows tools (e.g., IDE plugins)
    // to generate temporary files inside the base path
    source = fileTree(layout.projectDirectory.dir("src/commonMain/antlr")) {
        include("**/*.g4")
    }

    // We want the generated source files to have this package name
    val pkgName = "org.giraffemail.xcode.generated"
    packageName = pkgName

    // We want visitors alongside listeners.
    // The Kotlin target language is implicit, as is the file encoding (UTF-8)
    arguments = listOf("-visitor")

    // Generated files are outputted inside build/generatedAntlr/{package-name}
    val outDir = "generatedAntlr/${pkgName.replace(".", "/")}"
    outputDirectory = layout.buildDirectory.dir(outDir).get().asFile
}

// Task to fix deprecated UNSAFE_CALL suppressions in generated ANTLR code
val fixAntlrWarnings = tasks.register("fixAntlrWarnings") {
    dependsOn(generateKotlinGrammarSource)
    
    doLast {
        val generatedDir = file("build/generatedAntlr/org/giraffemail/xcode/generated")
        if (generatedDir.exists()) {
            generatedDir.listFiles { file -> file.name.endsWith("Parser.kt") }?.forEach { file ->
                val content = file.readText()
                val fixedContent = content.replace(
                    "@Suppress(\"UNSAFE_CALL\")",
                    "@Suppress(\"UNNECESSARY_NOT_NULL_ASSERTION\")"
                )
                if (content != fixedContent) {
                    file.writeText(fixedContent)
                    println("Fixed UNSAFE_CALL suppressions in ${file.name}")
                }
            }
        }
    }
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
                implementation("com.strumenta:antlr-kotlin-runtime:${antlrKotlinVersion}")
            }

            kotlin {
                srcDir(generateKotlinGrammarSource)
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

// Make sure ANTLR warnings are fixed before compilation
tasks.configureEach {
    if (name.startsWith("compile") && name.contains("Kotlin")) {
        dependsOn(fixAntlrWarnings)
    }
}

// Task to pre-warm Gradle and download dependencies for faster Qodana runs
tasks.register("qodanaPrep") {
    group = "verification"
    description = "Prepares project for Qodana analysis by pre-downloading dependencies"
    
    doFirst {
        println("Preparing project for Qodana analysis...")
    }
    
    // This task just ensures dependencies are resolved without full compilation
    dependsOn("resolveDependencies")
}

// Task to resolve all dependencies
tasks.register("resolveDependencies") {
    doLast {
        configurations.forEach { config ->
            if (config.isCanBeResolved) {
                try {
                    config.resolve()
                } catch (e: Exception) {
                    // Some configurations might not be resolvable in all contexts
                    logger.debug("Could not resolve configuration ${config.name}: ${e.message}")
                }
            }
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

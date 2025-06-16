# Xcode Project

## Overview

This project, internally referred to as "xcode" (as per `group = "org.giraffemail.xcode"`), is a source code transpiler built using Kotlin Multiplatform. It leverages ANTLR for parsing various programming languages and aims to generate executable binaries for multiple native targets including Linux, macOS (Intel), and Windows.

The core functionality revolves around parsing source code from languages like Java, JavaScript, and Python, and then likely transforming or transpiling this code.

## Current Functionality

*   **Multi-Language Parsing:**
    *   Java: Utilizes `Java.g4` ANTLR grammar.
    *   JavaScript: Utilizes `JavaScript.g4` ANTLR grammar.
    *   Python: Utilizes `Python.g4` ANTLR grammar, including indentation handling.
*   **Code Generation:** The project includes components for generating code, as suggested by test files like `JavaScriptGeneratorTest.kt` and `PythonGeneratorTest.kt`.
*   **Transpilation:** Core transpilation logic is being developed and tested (`TranspilationTest.kt`).
*   **Native Compilation:** Builds native executables for:
    *   Linux (x64)
    *   Windows (MinGW x64)
    *   macOS (x64, Intel)
*   **Testing Framework:** Comprehensive tests are in place for parsing, generation, and transpilation for each supported language.

## Architecture

*   **Kotlin Multiplatform (KMP):** The project is structured as a KMP application, allowing common logic to be shared across different native targets.
    *   Common logic and ANTLR grammars are located in `src/commonMain/`.
    *   Platform-specific entry points (`Main.kt`) are in `src/nativeLinuxMain/`, `src/nativeMacosMain/`, and `src/nativeWindowsMain/`.
*   **ANTLR for Parsing:**
    *   Grammar files (`.g4`) for Java, JavaScript, and Python are located in `src/commonMain/antlr/`.
    *   The `com.strumenta.antlr-kotlin` Gradle plugin is used to generate Kotlin-based parsers from these grammars. Generated sources are placed into `build/generatedAntlr/`.
*   **Gradle Build System:** The project uses Gradle with Kotlin DSL (`build.gradle.kts`) for managing dependencies, build configurations, and ANTLR code generation tasks.
*   **Modular Design:** The codebase appears to be organized by functionality (e.g., `javaparser`, `javascriptparser`, `pythonparser`, `transpiler`) within the `org.giraffemail.xcode` package structure.

## Limitations

*   **macOS ARM Support:** Support for macOS ARM (Apple Silicon) is currently commented out in the build configuration and would need to be enabled and tested.
*   **Language Feature Coverage:** The extent of supported features and versions for Java, JavaScript, and Python parsing and transpilation might be limited to specific subsets. Detailed documentation on supported language constructs is not yet available.
*   **Error Handling and Reporting:** While parsing and transpilation are implemented, the sophistication of error handling and reporting for invalid input code is not detailed.
*   **Output Targets:** The exact nature and target languages/formats of the "generation" and "transpilation" processes are not explicitly defined in the available context, beyond producing native executables.

## Acknowledgement

*   This project utilizes [ANTLR](https://www.antlr.org/) for language parsing.
*   The Kotlin integration for ANTLR is facilitated by the [com.strumenta.antlr-kotlin](https://github.com/Strumenta/antlr-kotlin) Gradle plugin.
*   Built with the [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) technology.

---
*This README was auto-generated based on the project structure and build files.*


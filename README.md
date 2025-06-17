# Xcode Project

## Overview

This project, internally referred to as "xcode" (as per `group = "org.giraffemail.xcode"`), is a source code transpiler built using Kotlin Multiplatform. It leverages ANTLR for parsing various programming languages and provides a command-line interface for transpiling code between different languages.

The core functionality revolves around parsing source code from languages like Java, JavaScript, Python, and TypeScript, and transpiling this code to any other supported language while preserving metadata and structure.

## Usage

The project builds native executables that provide a command-line interface for transpilation:

```bash
# Build the project
./gradlew build

# Basic usage examples
./build/bin/nativeLinux/releaseExecutable/xcode.kexe -t javascript example.py
./build/bin/nativeLinux/releaseExecutable/xcode.kexe -s java -t python -o output.py MyClass.java
./build/bin/nativeLinux/releaseExecutable/xcode.kexe --target typescript script.js

# Get help
./build/bin/nativeLinux/releaseExecutable/xcode.kexe --help
```

### CLI Options

- `-s, --source <lang>`: Source language (auto-detected from file extension if not specified)
- `-t, --target <lang>`: Target language (required)
- `-o, --output <path>`: Output file path (auto-generated if not specified)
- `-h, --help`: Show help message

### Supported Languages

- `python` (.py files)
- `javascript` (.js, .mjs files) 
- `java` (.java files)
- `typescript` (.ts files)

## Current Functionality

*   **Multi-Language Parsing:**
    *   Java: Utilizes `Java.g4` ANTLR grammar.
    *   JavaScript: Utilizes `JavaScript.g4` ANTLR grammar.
    *   Python: Utilizes `Python.g4` ANTLR grammar, including indentation handling.
    *   TypeScript: Utilizes `TypeScript.g4` ANTLR grammar with type annotation support.
*   **Code Generation:** The project includes components for generating code in all supported languages.
*   **Transpilation:** Full transpilation support between all language pairs with metadata preservation.
*   **Native CLI:** Command-line interface with cross-platform file I/O operations.
*   **Native Compilation:** Builds native executables for:
    *   Linux (x64)
    *   Windows (MinGW x64)
    *   macOS (x64, Intel)
*   **Testing Framework:** Comprehensive tests are in place for parsing, generation, transpilation, and CLI functionality.

## Architecture

*   **Kotlin Multiplatform (KMP):** The project is structured as a KMP application, allowing common logic to be shared across different native targets.
    *   Common logic and ANTLR grammars are located in `src/commonMain/`.
    *   Platform-specific entry points (`Main.kt`) are in `src/nativeLinuxMain/`, `src/nativeMacosMain/`, and `src/nativeWindowsMain/`.
    *   Cross-platform CLI implementation with expect/actual pattern for file I/O operations.
*   **ANTLR for Parsing:**
    *   Grammar files (`.g4`) for Java, JavaScript, Python, and TypeScript are located in `src/commonMain/antlr/`.
    *   The `com.strumenta.antlr-kotlin` Gradle plugin is used to generate Kotlin-based parsers from these grammars. Generated sources are placed into `build/generatedAntlr/`.
*   **Gradle Build System:** The project uses Gradle with Kotlin DSL (`build.gradle.kts`) for managing dependencies, build configurations, and ANTLR code generation tasks.
*   **Modular Design:** The codebase is organized by functionality:
    *   Language parsers and generators: `javaparser`, `javascriptparser`, `pythonparser`, `typescriptparser`
    *   AST definitions: `ast` package with node types and metadata support
    *   CLI interface: `cli` package with argument parsing and file operations
    *   Transpilation logic: `transpiler` tests demonstrating cross-language compilation

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


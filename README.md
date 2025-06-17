# Xcode Project

## Overview

This project is a sophisticated **cross-language source code transpiler** built using Kotlin Multiplatform. It enables seamless conversion between Java, JavaScript, Python, and TypeScript while preserving metadata and language-specific type information through an innovative parts-based metadata system.

**Key Features:**
- **Multi-directional transpilation** between 4 major programming languages
- **Metadata preservation** that maintains type information across language boundaries  
- **Native CLI** with cross-platform executables for Linux, Windows, and macOS
- **ANTLR-powered parsing** with robust grammar support for each language
- **Round-trip transpilation** that preserves original semantics and typing

The transpiler is particularly valuable for:
- **Cross-platform code sharing** - convert algorithms between different technology stacks
- **Language migration** - gradually migrate codebases from one language to another
- **Polyglot development** - maintain consistent logic across multiple language implementations
- **Educational purposes** - understand how similar concepts are expressed in different languages

## Installation and Usage

### Building the Project

```bash
# Clone the repository
git clone https://github.com/pydlv/xcode.git
cd xcode

# Build the project (generates native executables)
./gradlew build
```

This creates native executables for different platforms:
- Linux: `./build/bin/nativeLinux/releaseExecutable/xcode.kexe`
- Windows: `./build/bin/nativeWindows/releaseExecutable/xcode.exe`
- macOS: `./build/bin/nativeMacos/releaseExecutable/xcode.kexe`

### CLI Usage

```bash
# Basic transpilation (source language auto-detected from file extension)
xcode -t javascript example.py           # Python to JavaScript
xcode -t python script.js               # JavaScript to Python  
xcode --target typescript MyClass.java  # Java to TypeScript

# Specify source language explicitly
xcode -s java -t python MyClass.java    # Java to Python

# Specify output file
xcode -s python -t java -o MyClass.java script.py

# Get help
xcode --help
```

### CLI Options

- `-s, --source <lang>`: Source language (auto-detected from file extension if not specified)
- `-t, --target <lang>`: Target language (required)
- `-o, --output <path>`: Output file path (auto-generated if not specified)
- `-h, --help`: Show help message

### Supported Languages

- `python` - Python files (.py)
- `javascript` - JavaScript files (.js, .mjs) 
- `java` - Java files (.java)
- `typescript` - TypeScript files (.ts)

### Transpilation Examples

#### Python to JavaScript
**Input (example.py):**
```python
def greet(name):
    print("Hello, " + name)
```

**Output (example.js):**
```javascript
function greet(name) {
    console.log('Hello, ' + name);
}
```

#### TypeScript to Python (with type preservation)
**Input (simple.ts):**
```typescript
function greet(name: string): void {
    console.log("Hello");
}
```

**Output (simple.py):**
```python
def greet(name):
    print('Hello')
```

The transpiler preserves type information internally and can restore it when transpiling back to typed languages.

## Metadata Preservation

One of the key features of this transpiler is its sophisticated **metadata preservation system**. When transpiling code between languages, the system preserves type information, parameter types, and other language-specific metadata, even when converting to languages that don't natively support those features.

### How Metadata Preservation Works

1. **Extraction**: When parsing source code, the transpiler extracts type annotations, parameter types, and other metadata
2. **Storage**: Metadata is stored separately from the generated code using a "parts-based" system
3. **Restoration**: When transpiling back to a typed language, the metadata is used to restore type annotations

### Example: Round-trip Type Preservation

**Original TypeScript:**
```typescript
function greet(name: string): void {
    console.log("Hello");
}
```

**Intermediate JavaScript (metadata preserved internally):**
```javascript
function greet(name) {
    console.log('Hello');
}
```

**Back to TypeScript (types restored):**
```typescript
function greet(name: string): void {
    console.log("Hello");
}
```

This enables seamless transpilation chains like TypeScript → JavaScript → Python → TypeScript while preserving all type information.

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

## Testing

The project includes comprehensive test suites covering all major functionality:

```bash
# Run all tests
./gradlew test

# Run only Linux native tests (faster)
./gradlew nativeLinuxTest

# Build and run tests
./gradlew build
```

**Test Coverage:**
- Parser tests for all supported languages
- Code generation and transpilation tests  
- Metadata preservation and round-trip tests
- CLI interface and file I/O tests
- Cross-language transpilation chains

## Development

### Prerequisites
- JDK 17 or later
- Gradle 8.0 or later (included via wrapper)

### Running Quality Checks
```bash
# Run Qodana code quality analysis
./.github/run-qodana-scan.sh
```

## Documentation

For detailed language feature roadmaps and implementation status:

📚 **[Language Documentation](docs/README.md)**

- [Java Language Features](docs/java-features.md)
- [JavaScript Language Features](docs/javascript-features.md)  
- [Python Language Features](docs/python-features.md)
- [TypeScript Language Features](docs/typescript-features.md)
- [Transpilation Features](docs/transpilation-features.md)

Each feature roadmap includes links to test files demonstrating functionality.

## Acknowledgements

*   This project utilizes [ANTLR](https://www.antlr.org/) for language parsing.
*   The Kotlin integration for ANTLR is facilitated by the [com.strumenta.antlr-kotlin](https://github.com/Strumenta/antlr-kotlin) Gradle plugin.
*   Built with the [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) technology.

## License

See [LICENSE](LICENSE) file for details.


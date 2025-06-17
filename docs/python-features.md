# Python Language Features

This document outlines the Python language features currently supported by the Xcode transpiler.

## ✅ Implemented Features

### Core Language Constructs

#### I/O Operations
- **Print Statements** - Support for Python print function
  - Test: [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L42-L61) - `test parsing hello world program`
  - Test: [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L64-L84) - `test parsing print with arbitrary string`
  - Features: `print()` function with string literals

#### Expressions and Operations
- **Binary Operations** - Support for basic arithmetic operations
  - Test: [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L87-L108) - `test parsing print with simple addition`
  - Features: Addition operator `+` with numeric operands in print statements

#### Function Calls
- **Function Invocation** - Support for calling functions
  - Test: [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L111-L134) - `test parsing fibonacci call with integer arguments`
  - Features: Function calls with multiple integer arguments

#### Literals and Constants
- **String Literals** - Support for string constants
  - Test: [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L49-L50) - String literal "Hello, World!"
  - Features: Single-quoted string literals

- **Numeric Constants** - Support for integer literals
  - Test: [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L119-L121) - Integer arguments `0, 1`
  - Features: Integer literal parsing and handling

#### Empty Modules
- **Empty Code Handling** - Support for parsing empty Python files
  - Test: [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L28-L39) - `test parsing empty string`
  - Features: Empty module parsing with minimal AST generation

### Language-Specific Features

#### Indentation Handling
- **Python Indentation** - Proper handling of Python's indentation-based syntax
  - Test: [PythonIndentationHandlerTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonIndentationHandlerTest.kt) - Indentation handling tests
  - Features: Python-specific indentation processing

### Code Generation
- **Python Code Generation** - Ability to generate Python code from AST
  - Test: [PythonGeneratorTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonGeneratorTest.kt) - Python code generation tests
  - Features: Converting AST back to Python source code

### Transpilation Support
- **Cross-Language Transpilation** - Support for transpiling to/from Python
  - Test: [TranspilationTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/transpiler/TranspilationTest.kt#L36-L40) - Python language configuration
  - Features: Round-trip transpilation with metadata preservation

### Metadata Preservation
- **Type Information Preservation** - Maintains type information during transpilation
  - Test: [PartsBasedMetadataTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/metadata/PartsBasedMetadataTest.kt#L205-L226) - Python code generation with metadata
  - Features: Parts-based metadata system for cross-language type preservation

### Error Handling
- **Parse Error Management** - Structured error handling for invalid Python code
  - Test: [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L12-L25) - `test parsing specific input that triggers error`
  - Features: AstParseException handling with descriptive error messages

## 🚧 Planned Features

### Core Language Constructs
- **Function Definitions** - def function declarations
- **Variable Assignments** - Variable assignment operations
- **Class Definitions** - Class declarations and methods
- **Import Statements** - Module import support

### Data Types and Structures
- **Lists** - List literals and operations
- **Dictionaries** - Dict literals and operations
- **Tuples** - Tuple syntax and operations
- **Sets** - Set literals and operations
- **Comprehensions** - List, dict, set comprehensions

### Control Flow
- **Conditional Statements** - if-elif-else statements
- **Loops** - for, while loops
- **Exception Handling** - try-except-finally blocks
- **With Statements** - Context manager support

### Advanced Features
- **Decorators** - Function and class decorators
- **Generators** - Generator functions and expressions
- **Lambda Functions** - Anonymous function support
- **F-strings** - Formatted string literals

### Python-Specific Features
- **Multiple Assignment** - Tuple unpacking
- **Slicing** - Sequence slicing operations
- **Duck Typing** - Dynamic typing support
- **Magic Methods** - Dunder method support

## 📊 Implementation Status

| Feature Category | Implemented | Planned | Total |
|-----------------|-------------|---------|-------|
| Core Constructs | 5 | 4 | 9 |
| Data Types | 2 | 5 | 7 |
| Control Flow | 0 | 4 | 4 |
| Advanced | 2 | 4 | 6 |
| Python-Specific | 1 | 4 | 5 |
| **Total** | **10** | **21** | **31** |

## 🔗 Related Documentation

- [Java Features](java-features.md)
- [JavaScript Features](javascript-features.md)
- [TypeScript Features](typescript-features.md)
- [Transpilation Features](transpilation-features.md)
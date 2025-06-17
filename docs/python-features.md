# Python Language Features

This document outlines the Python language features currently supported by the Xcode transpiler.

## ✅ Implemented Features

| Feature | Category | Description | Test Link |
|---------|----------|-------------|-----------|
| Print Statements | Core Language Constructs | Support for Python print function (`print()` function with string literals) | [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L42-L61) |
| Binary Operations | Core Language Constructs | Support for basic arithmetic operations (Addition operator `+` with numeric operands in print statements) | [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L87-L108) |
| Function Invocation | Core Language Constructs | Support for calling functions (Function calls with multiple integer arguments) | [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L111-L134) |
| String Literals | Literals and Constants | Support for string constants (Single-quoted string literals) | [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L49-L50) |
| Numeric Constants | Literals and Constants | Support for integer literals (Integer literal parsing and handling) | [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L119-L121) |
| Empty Code Handling | Core Language Constructs | Support for parsing empty Python files (Empty module parsing with minimal AST generation) | [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L28-L39) |
| Python Indentation | Language-Specific Features | Proper handling of Python's indentation-based syntax (Python-specific indentation processing) | [PythonIndentationHandlerTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonIndentationHandlerTest.kt) |
| Python Code Generation | Code Generation | Ability to generate Python code from AST (Converting AST back to Python source code) | [PythonGeneratorTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonGeneratorTest.kt) |
| Cross-Language Transpilation | Transpilation Support | Support for transpiling to/from Python (Round-trip transpilation with metadata preservation) | [TranspilationTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/transpiler/TranspilationTest.kt#L36-L40) |
| Type Information Preservation | Metadata Preservation | Maintains type information during transpilation (Parts-based metadata system for cross-language type preservation) | [PartsBasedMetadataTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/metadata/PartsBasedMetadataTest.kt#L205-L226) |
| Parse Error Management | Error Handling | Structured error handling for invalid Python code (AstParseException handling with descriptive error messages) | [PythonParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/pythonparser/PythonParserTest.kt#L12-L25) |

## 🚧 Planned Features

| Feature | Category | Description | Create Issue |
|---------|----------|-------------|--------------|
| Function Definitions | Core Language Constructs | def function declarations | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Function+Definitions&body=Add+support+for+Python+function+definitions+including+def+keyword,+parameters,+and+return+statements.&labels=enhancement,python) |
| Variable Assignments | Core Language Constructs | Variable assignment operations | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Variable+Assignments&body=Add+support+for+Python+variable+assignments+including+simple+and+augmented+assignment+operators.&labels=enhancement,python) |
| Class Definitions | Core Language Constructs | Class declarations and methods | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Class+Definitions&body=Add+support+for+Python+class+definitions+including+class+keyword,+methods,+and+inheritance.&labels=enhancement,python) |
| Import Statements | Core Language Constructs | Module import support | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Import+Statements&body=Add+support+for+Python+import+statements+including+import,+from-import,+and+relative+imports.&labels=enhancement,python) |
| Lists | Data Types and Structures | List literals and operations | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Lists&body=Add+support+for+Python+lists+including+list+literals,+indexing,+and+common+list+methods.&labels=enhancement,python) |
| Dictionaries | Data Types and Structures | Dict literals and operations | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Dictionaries&body=Add+support+for+Python+dictionaries+including+dict+literals,+key+access,+and+common+dict+methods.&labels=enhancement,python) |
| Tuples | Data Types and Structures | Tuple syntax and operations | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Tuples&body=Add+support+for+Python+tuples+including+tuple+literals,+indexing,+and+unpacking.&labels=enhancement,python) |
| Sets | Data Types and Structures | Set literals and operations | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Sets&body=Add+support+for+Python+sets+including+set+literals,+set+operations,+and+common+set+methods.&labels=enhancement,python) |
| Comprehensions | Data Types and Structures | List, dict, set comprehensions | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Comprehensions&body=Add+support+for+Python+comprehensions+including+list,+dict,+and+set+comprehensions+with+filtering.&labels=enhancement,python) |
| Conditional Statements | Control Flow | if-elif-else statements | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Conditional+Statements&body=Add+support+for+Python+conditional+statements+including+if-elif-else+constructs.&labels=enhancement,python) |
| Loops | Control Flow | for, while loops | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Loops&body=Add+support+for+Python+loops+including+for+and+while+loops+with+break+and+continue.&labels=enhancement,python) |
| Exception Handling | Control Flow | try-except-finally blocks | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Exception+Handling&body=Add+support+for+Python+exception+handling+including+try-except-finally+blocks+and+raise+statements.&labels=enhancement,python) |
| With Statements | Control Flow | Context manager support | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+With+Statements&body=Add+support+for+Python+with+statements+for+context+manager+protocol.&labels=enhancement,python) |
| Decorators | Advanced Features | Function and class decorators | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Decorators&body=Add+support+for+Python+decorators+including+function+and+class+decorators+with+arguments.&labels=enhancement,python) |
| Generators | Advanced Features | Generator functions and expressions | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Generators&body=Add+support+for+Python+generators+including+yield+statements+and+generator+expressions.&labels=enhancement,python) |
| Lambda Functions | Advanced Features | Anonymous function support | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Lambda+Functions&body=Add+support+for+Python+lambda+functions+for+anonymous+function+expressions.&labels=enhancement,python) |
| F-strings | Advanced Features | Formatted string literals | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+F-strings&body=Add+support+for+Python+f-strings+for+formatted+string+literals+with+expression+interpolation.&labels=enhancement,python) |
| Multiple Assignment | Python-Specific Features | Tuple unpacking | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Multiple+Assignment&body=Add+support+for+Python+multiple+assignment+and+tuple+unpacking+operations.&labels=enhancement,python) |
| Slicing | Python-Specific Features | Sequence slicing operations | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Slicing&body=Add+support+for+Python+sequence+slicing+operations+with+start,+stop,+and+step+parameters.&labels=enhancement,python) |
| Duck Typing | Python-Specific Features | Dynamic typing support | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Duck+Typing&body=Add+support+for+Python+duck+typing+and+dynamic+typing+features.&labels=enhancement,python) |
| Magic Methods | Python-Specific Features | Dunder method support | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Python+Magic+Methods&body=Add+support+for+Python+magic+methods+(dunder+methods)+including+__init__,+__str__,+etc.&labels=enhancement,python) |

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
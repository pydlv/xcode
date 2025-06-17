# Java Language Features

This document outlines the Java language features currently supported by the Xcode transpiler.

## ✅ Implemented Features

### Core Language Constructs

#### Functions/Methods
- **Function Declarations** - Support for Java method declarations with parameters
  - Test: [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L10-L65) - `test parse recursive fibonacci function`
  - Features: `public static void` method declarations, parameter lists with `Object` types

#### Variables and Assignments  
- **Variable Assignment** - Support for variable assignment operations
  - Test: [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L19-L26) - Assignment in fibonacci function
  - Features: Simple variable assignment with `c = a + b` syntax

#### Expressions and Operations
- **Binary Operations** - Support for basic arithmetic operations
  - Test: [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L21-L25) - Addition operation in fibonacci
  - Features: Addition operator `+` between variables

#### Function Calls
- **Method Invocation** - Support for calling methods/functions
  - Test: [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L30-L39) - Recursive fibonacci call
  - Test: [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L50-L59) - Initial fibonacci call
  - Features: Function calls with multiple arguments, recursive calls

#### I/O Operations
- **Print Statements** - Support for output operations
  - Test: [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L27-L29) - System.out.println
  - Features: `System.out.println()` statement parsing

#### Literals and Constants
- **Numeric Constants** - Support for integer literals
  - Test: [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L54-L56) - Integer arguments `0, 1`
  - Features: Integer literal parsing and handling

### Code Generation
- **Java Code Generation** - Ability to generate Java code from AST
  - Test: [JavaGeneratorTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaGeneratorTest.kt) - Java code generation tests
  - Features: Converting AST back to Java source code

### Transpilation Support
- **Cross-Language Transpilation** - Support for transpiling to/from Java
  - Test: [TranspilationTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/transpiler/TranspilationTest.kt#L44-L48) - Java language configuration
  - Features: Round-trip transpilation with metadata preservation

## 🚧 Planned Features

### Advanced Language Constructs
- **Classes and Objects** - Object-oriented programming support
- **Interfaces** - Interface declarations and implementations  
- **Inheritance** - Class inheritance and method overriding
- **Access Modifiers** - private, protected, public access control
- **Static Members** - Static methods and fields
- **Exception Handling** - try-catch-finally blocks

### Data Types and Structures
- **Primitive Types** - int, long, double, boolean, char
- **Arrays** - Array declarations and operations
- **Collections** - List, Set, Map support
- **Generics** - Generic type parameters

### Control Flow
- **Conditional Statements** - if-else statements
- **Loops** - for, while, do-while loops
- **Switch Statements** - Switch-case constructs
- **Break/Continue** - Loop control statements

### Advanced Features
- **Annotations** - Annotation declarations and usage
- **Lambda Expressions** - Java 8+ lambda support
- **Stream API** - Java 8+ stream operations
- **Module System** - Java 9+ module declarations

## 📊 Implementation Status

| Feature Category | Implemented | Planned | Total |
|-----------------|-------------|---------|-------|
| Core Constructs | 6 | 6 | 12 |
| Data Types | 1 | 7 | 8 |
| Control Flow | 0 | 5 | 5 |
| Advanced | 0 | 4 | 4 |
| **Total** | **7** | **22** | **29** |

## 🔗 Related Documentation

- [JavaScript Features](javascript-features.md)
- [Python Features](python-features.md) 
- [TypeScript Features](typescript-features.md)
- [Transpilation Features](transpilation-features.md)
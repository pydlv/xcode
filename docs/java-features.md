# Java Language Features

This document outlines the Java language features currently supported by the Xcode transpiler.

## ✅ Implemented Features

| Feature | Category | Description | Test Link |
|---------|----------|-------------|-----------|
| Function Declarations | Core Language Constructs | Support for Java method declarations with parameters (`public static void` method declarations, parameter lists with `Object` types) | [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L10-L65) |
| Variable Assignment | Core Language Constructs | Support for variable assignment operations (Simple variable assignment with `c = a + b` syntax) | [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L19-L26) |
| Binary Operations | Core Language Constructs | Support for basic arithmetic operations (Addition operator `+` between variables) | [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L21-L25) |
| Method Invocation | Core Language Constructs | Support for calling methods/functions (Function calls with multiple arguments, recursive calls) | [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L30-L39) |
| Print Statements | I/O Operations | Support for output operations (`System.out.println()` statement parsing) | [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L27-L29) |
| Numeric Constants | Literals and Constants | Support for integer literals (Integer literal parsing and handling) | [JavaParserTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaParserTest.kt#L54-L56) |
| Java Code Generation | Code Generation | Ability to generate Java code from AST (Converting AST back to Java source code) | [JavaGeneratorTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/javaparser/JavaGeneratorTest.kt) |
| Cross-Language Transpilation | Transpilation Support | Support for transpiling to/from Java (Round-trip transpilation with metadata preservation) | [TranspilationTest.kt](../src/commonTest/kotlin/org/giraffemail/xcode/transpiler/TranspilationTest.kt#L44-L48) |

## 🚧 Planned Features

| Feature | Category | Description | Create Issue |
|---------|----------|-------------|--------------|
| Classes and Objects | Advanced Language Constructs | Object-oriented programming support | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Classes+and+Objects&body=Add+support+for+Java+classes+and+objects+including+class+declarations,+constructors,+instance+methods,+and+field+access.&labels=enhancement,java) |
| Interfaces | Advanced Language Constructs | Interface declarations and implementations | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Interfaces&body=Add+support+for+Java+interface+declarations+and+implementations+including+abstract+methods+and+default+methods.&labels=enhancement,java) |
| Inheritance | Advanced Language Constructs | Class inheritance and method overriding | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Inheritance&body=Add+support+for+Java+class+inheritance+including+extends+keyword,+super+calls,+and+method+overriding.&labels=enhancement,java) |
| Access Modifiers | Advanced Language Constructs | private, protected, public access control | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Access+Modifiers&body=Add+support+for+Java+access+modifiers+including+private,+protected,+public,+and+package-private+visibility.&labels=enhancement,java) |
| Static Members | Advanced Language Constructs | Static methods and fields | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Static+Members&body=Add+support+for+Java+static+methods+and+fields+including+static+initialization+blocks.&labels=enhancement,java) |
| Exception Handling | Advanced Language Constructs | try-catch-finally blocks | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Exception+Handling&body=Add+support+for+Java+exception+handling+including+try-catch-finally+blocks,+throw+statements,+and+custom+exceptions.&labels=enhancement,java) |
| Primitive Types | Data Types and Structures | int, long, double, boolean, char | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Primitive+Types&body=Add+support+for+Java+primitive+types+including+int,+long,+double,+boolean,+char,+byte,+short,+and+float.&labels=enhancement,java) |
| Arrays | Data Types and Structures | Array declarations and operations | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Arrays&body=Add+support+for+Java+arrays+including+array+declarations,+initialization,+indexing,+and+multi-dimensional+arrays.&labels=enhancement,java) |
| Collections | Data Types and Structures | List, Set, Map support | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Collections&body=Add+support+for+Java+collections+including+List,+Set,+Map,+ArrayList,+HashMap,+and+collection+operations.&labels=enhancement,java) |
| Generics | Data Types and Structures | Generic type parameters | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Generics&body=Add+support+for+Java+generics+including+type+parameters,+bounded+types,+wildcards,+and+generic+methods.&labels=enhancement,java) |
| Conditional Statements | Control Flow | if-else statements | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Conditional+Statements&body=Add+support+for+Java+conditional+statements+including+if-else+constructs+and+ternary+operators.&labels=enhancement,java) |
| Loops | Control Flow | for, while, do-while loops | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Loops&body=Add+support+for+Java+loops+including+for,+while,+do-while,+and+enhanced+for+loops.&labels=enhancement,java) |
| Switch Statements | Control Flow | Switch-case constructs | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Switch+Statements&body=Add+support+for+Java+switch+statements+including+case+labels,+default+cases,+and+switch+expressions.&labels=enhancement,java) |
| Break/Continue | Control Flow | Loop control statements | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Break+Continue&body=Add+support+for+Java+break+and+continue+statements+for+loop+control+including+labeled+breaks.&labels=enhancement,java) |
| Annotations | Advanced Features | Annotation declarations and usage | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Annotations&body=Add+support+for+Java+annotations+including+annotation+declarations,+built-in+annotations,+and+custom+annotations.&labels=enhancement,java) |
| Lambda Expressions | Advanced Features | Java 8+ lambda support | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Lambda+Expressions&body=Add+support+for+Java+8+lambda+expressions+including+functional+interfaces+and+method+references.&labels=enhancement,java) |
| Stream API | Advanced Features | Java 8+ stream operations | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Stream+API&body=Add+support+for+Java+8+Stream+API+including+stream+operations,+collectors,+and+parallel+streams.&labels=enhancement,java) |
| Module System | Advanced Features | Java 9+ module declarations | [📝 Create Issue](https://github.com/pydlv/xcode/issues/new?title=Implement+Java+Module+System&body=Add+support+for+Java+9+module+system+including+module+declarations,+exports,+and+requires+directives.&labels=enhancement,java) |

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
package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.ast.*

/**
 * Enumeration of supported AST features for selective generation
 */
enum class AstFeature {
    FUNCTION_DEFINITIONS,
    CLASS_DEFINITIONS,
    VARIABLE_ASSIGNMENTS,
    BINARY_OPERATIONS,
    PRINT_STATEMENTS,
    FUNCTION_CALLS,
    CONSTANT_VALUES,
    VARIABLE_REFERENCES,
    CONDITIONAL_STATEMENTS,
    COMPARISON_OPERATIONS,
    RETURN_STATEMENTS,
    ARRAY_LITERALS,
    TUPLE_LITERALS,
    FOR_EACH_LOOPS
}

/**
 * Supported common AST features for maximal AST generation
 */
object SupportedAstFeatures {

    /**
     * Set of all supported AST features
     */
    val ALL_FEATURES = setOf(
        AstFeature.FUNCTION_DEFINITIONS,
        AstFeature.CLASS_DEFINITIONS,
        AstFeature.VARIABLE_ASSIGNMENTS,
        AstFeature.BINARY_OPERATIONS,
        AstFeature.PRINT_STATEMENTS,
        AstFeature.FUNCTION_CALLS,
        AstFeature.CONSTANT_VALUES,
        AstFeature.VARIABLE_REFERENCES,
        AstFeature.CONDITIONAL_STATEMENTS,
        AstFeature.COMPARISON_OPERATIONS,
        AstFeature.RETURN_STATEMENTS,
        AstFeature.ARRAY_LITERALS,
        AstFeature.TUPLE_LITERALS
    )

    /**
     * List of supported AST node types and their associated metadata features
     */
    val SUPPORTED_FEATURES = listOf(
        "Function definitions with typed parameters",
        "Class definitions with methods",
        "Function return type annotations",
        "Variable assignments with type annotations",
        "Function calls as statements",
        "Print/console.log statements",
        "Conditional statements (if/else)",
        "Binary operations (arithmetic, string)",
        "Comparison operations",
        "Constant values (strings, numbers)",
        "Variable references (Load, Store, Param contexts)",
        "Array literals with type preservation",
        "Tuple literals with mixed types"
    )

    /**
     * Supported metadata types from LanguageMetadata
     */
    val SUPPORTED_METADATA = listOf(
        "returnType" to "Function return type annotations",
        "paramTypes" to "Parameter type mappings",
        "variableType" to "Variable type annotations",
        "individualParamMetadata" to "Per-parameter detailed metadata"
    )
}

/**
 * Utility for generating maximal AST nodes with all supported features
 */
object MaximalAstGenerator {

    /**
     * Generates a maximal AST that includes specified AST features.
     * This AST can be used in tests to verify comprehensive transpilation support.
     * @param features Set of AST features to include. Defaults to all supported features.
     */
    fun generateMaximalAst(features: Set<AstFeature> = SupportedAstFeatures.ALL_FEATURES): ModuleNode {
        val bodyNodes = mutableListOf<StatementNode>()

        // Generate function definition if requested
        if (features.contains(AstFeature.FUNCTION_DEFINITIONS)) {
            val functionBody = mutableListOf<StatementNode>()

            // Add variable assignment within function if requested
            if (features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
                val assignValue = if (features.contains(AstFeature.BINARY_OPERATIONS)) {
                    BinaryOpNode(
                        left = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                            NameNode(id = "input", ctx = Load) else ConstantNode("input"),
                        op = "+",
                        right = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                            NameNode(id = "count", ctx = Load) else ConstantNode(1)
                    )
                } else if (features.contains(AstFeature.CONSTANT_VALUES)) {
                    ConstantNode("hello")
                } else {
                    NameNode(id = "input", ctx = Load)
                }

                functionBody.add(
                    AssignNode(
                        target = NameNode(id = "result", ctx = Store),
                        value = assignValue,
                        metadata = mapOf("variableType" to "string")
                    )
                )
            }

            // Add print statement if requested
            if (features.contains(AstFeature.PRINT_STATEMENTS)) {
                functionBody.add(
                    PrintNode(
                        expression = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                            NameNode(id = "result", ctx = Load) else ConstantNode("output")
                    )
                )
            }

            // Add return statement if requested
            if (features.contains(AstFeature.RETURN_STATEMENTS)) {
                val returnValue =
                    if (features.contains(AstFeature.VARIABLE_REFERENCES) && features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
                        // Return the result variable if we have assignments and variable references
                        NameNode(id = "result", ctx = Load)
                    } else if (features.contains(AstFeature.BINARY_OPERATIONS)) {
                        // Return a binary operation
                        BinaryOpNode(
                            left = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                                NameNode(id = "input", ctx = Load) else ConstantNode("a"),
                            op = "+",
                            right = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                                NameNode(id = "count", ctx = Load) else ConstantNode(1)
                        )
                    } else if (features.contains(AstFeature.CONSTANT_VALUES)) {
                        // Return a constant
                        ConstantNode("returned_value")
                    } else {
                        // Return null (void return)
                        null
                    }

                functionBody.add(ReturnNode(value = returnValue))
            }

            // Create function arguments based on features
            val functionArgs = if (features.contains(AstFeature.VARIABLE_REFERENCES)) {
                listOf(
                    NameNode(id = "input", ctx = Param, metadata = mapOf("type" to "string")),
                    NameNode(id = "count", ctx = Param, metadata = mapOf("type" to "number"))
                )
            } else {
                emptyList()
            }

            // Determine return type based on features
            val returnType = if (features.contains(AstFeature.RETURN_STATEMENTS)) {
                if (features.contains(AstFeature.VARIABLE_ASSIGNMENTS) && features.contains(AstFeature.VARIABLE_REFERENCES)) {
                    "string" // returning result variable
                } else if (features.contains(AstFeature.BINARY_OPERATIONS)) {
                    "number" // returning binary operation result
                } else if (features.contains(AstFeature.CONSTANT_VALUES)) {
                    "string" // returning constant value
                } else {
                    "void" // returning null
                }
            } else {
                "void"
            }

            bodyNodes.add(
                FunctionDefNode(
                    name = "processData",
                    args = functionArgs,
                    body = functionBody,
                    decoratorList = emptyList(),
                    metadata = mapOf(
                        "returnType" to returnType,
                        "paramTypes" to mapOf("input" to "string", "count" to "number")
                    )
                )
            )
        }

        // Generate class definition if requested
        if (features.contains(AstFeature.CLASS_DEFINITIONS)) {
            val classBody = mutableListOf<StatementNode>()

            // Add a method to the class if function definitions are also requested
            if (features.contains(AstFeature.FUNCTION_DEFINITIONS)) {
                val methodBody = mutableListOf<StatementNode>()

                // Add method body based on other features
                if (features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
                    methodBody.add(
                        AssignNode(
                            target = NameNode(id = "instanceValue", ctx = Store),
                            value = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                                NameNode(id = "newValue", ctx = Load) else ConstantNode("initialized"),
                            metadata = mapOf("variableType" to "string")
                        )
                    )
                }

                if (features.contains(AstFeature.RETURN_STATEMENTS)) {
                    methodBody.add(
                        ReturnNode(
                            value = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                                NameNode(id = "instanceValue", ctx = Load) else ConstantNode("method_result")
                        )
                    )
                }

                classBody.add(
                    FunctionDefNode(
                        name = "getValue",
                        args = if (features.contains(AstFeature.VARIABLE_REFERENCES)) {
                            listOf(NameNode(id = "newValue", ctx = Param, metadata = mapOf("type" to "string")))
                        } else {
                            emptyList()
                        },
                        body = methodBody,
                        decoratorList = emptyList(),
                        metadata = mapOf(
                            "returnType" to "string",
                            "paramTypes" to mapOf("newValue" to "string")
                        )
                    )
                )
            }

            // Add a simple method even if function definitions are not requested
            if (!features.contains(AstFeature.FUNCTION_DEFINITIONS) && features.contains(AstFeature.PRINT_STATEMENTS)) {
                classBody.add(
                    FunctionDefNode(
                        name = "display",
                        args = emptyList(),
                        body = listOf(
                            PrintNode(
                                expression = if (features.contains(AstFeature.CONSTANT_VALUES))
                                    ConstantNode("Class instance") else ConstantNode("Display method")
                            )
                        ),
                        decoratorList = emptyList(),
                        metadata = mapOf("returnType" to "void")
                    )
                )
            }

            // Always ensure class has at least one method to avoid empty class bodies
            if (classBody.isEmpty()) {
                classBody.add(
                    FunctionDefNode(
                        name = "defaultMethod",
                        args = emptyList(),
                        body = listOf(
                            PrintNode(expression = ConstantNode("Default class method"))
                        ),
                        decoratorList = emptyList(),
                        metadata = mapOf("returnType" to "void")
                    )
                )
            }

            bodyNodes.add(
                ClassDefNode(
                    name = "DataProcessor",
                    baseClasses = emptyList(), // No inheritance for simplicity
                    body = classBody,
                    decoratorList = emptyList(),
                    metadata = mapOf(
                        "classType" to "DataProcessor",
                        "methods" to classBody.filterIsInstance<FunctionDefNode>().map { it.name }
                    )
                )
            )
        }

        // Generate standalone variable assignment if requested and not already in function
        if (features.contains(AstFeature.VARIABLE_ASSIGNMENTS) && !features.contains(AstFeature.FUNCTION_DEFINITIONS)) {
            val assignValue = if (features.contains(AstFeature.CONSTANT_VALUES)) {
                ConstantNode("standalone")
            } else {
                NameNode(id = "defaultValue", ctx = Load)
            }

            bodyNodes.add(
                AssignNode(
                    target = NameNode(id = "standalone", ctx = Store),
                    value = assignValue,
                    metadata = mapOf("variableType" to "string")
                )
            )
        }

        // Generate function call statement if requested
        if (features.contains(AstFeature.FUNCTION_CALLS)) {
            val callArgs = if (features.contains(AstFeature.CONSTANT_VALUES)) {
                listOf(ConstantNode("hello"), ConstantNode(42))
            } else {
                emptyList()
            }

            bodyNodes.add(
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "processData", ctx = Load),
                        args = callArgs,
                        keywords = emptyList()
                    )
                )
            )
        }

        // Generate standalone conditional statement if requested
        if (features.contains(AstFeature.CONDITIONAL_STATEMENTS) && !features.contains(AstFeature.FUNCTION_DEFINITIONS)) {
            // Create test condition - use comparison if available
            val testCondition = if (features.contains(AstFeature.COMPARISON_OPERATIONS)) {
                CompareNode(
                    left = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                        NameNode(id = "x", ctx = Load) else ConstantNode(10),
                    op = ">",
                    right = if (features.contains(AstFeature.CONSTANT_VALUES))
                        ConstantNode(5) else ConstantNode(0)
                )
            } else {
                // Simple boolean variable or constant
                if (features.contains(AstFeature.VARIABLE_REFERENCES))
                    NameNode(id = "condition", ctx = Load) else ConstantNode(true)
            }

            // Simple if-else with print statements
            bodyNodes.add(
                IfNode(
                    test = testCondition,
                    body = listOf(
                        PrintNode(
                            expression = if (features.contains(AstFeature.CONSTANT_VALUES))
                                ConstantNode("condition is true") else NameNode(id = "trueMessage", ctx = Load)
                        )
                    ),
                    orelse = listOf(
                        PrintNode(
                            expression = if (features.contains(AstFeature.CONSTANT_VALUES))
                                ConstantNode("condition is false") else NameNode(id = "falseMessage", ctx = Load)
                        )
                    )
                )
            )
        }

        // Generate standalone print statement if requested and not already covered
        if (features.contains(AstFeature.PRINT_STATEMENTS) &&
            !features.contains(AstFeature.FUNCTION_DEFINITIONS) &&
            !features.contains(AstFeature.CONDITIONAL_STATEMENTS)
        ) {
            bodyNodes.add(
                PrintNode(
                    expression = if (features.contains(AstFeature.CONSTANT_VALUES))
                        ConstantNode("standalone print") else NameNode(id = "printValue", ctx = Load)
                )
            )
        }

        // Generate array literal assignment if requested
        if (features.contains(AstFeature.ARRAY_LITERALS)) {
            val arrayElements = if (features.contains(AstFeature.CONSTANT_VALUES)) {
                listOf(ConstantNode("item1"), ConstantNode("item2"), ConstantNode("item3"))
            } else {
                listOf(
                    NameNode(id = "elem1", ctx = Load),
                    NameNode(id = "elem2", ctx = Load),
                    NameNode(id = "elem3", ctx = Load)
                )
            }

            // If in a function, add to function body; otherwise standalone
            if (features.contains(AstFeature.FUNCTION_DEFINITIONS) && bodyNodes.isNotEmpty()) {
                val function = bodyNodes.find { it is FunctionDefNode } as? FunctionDefNode
                if (function != null && features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
                    val newBody = function.body.toMutableList()
                    // Find the index of the return statement (if any)
                    val returnIndex = newBody.indexOfFirst { it is ReturnNode }
                    val insertIndex = if (returnIndex >= 0) returnIndex else newBody.size
                    // Insert before the return statement, or at the end if no return
                    newBody.add(
                        insertIndex,
                        AssignNode(
                            target = NameNode(id = "arrayData", ctx = Store),
                            value = ListNode(elements = arrayElements, metadata = mapOf("arrayType" to "string")),
                            metadata = mapOf("variableType" to "string[]")
                        )
                    )
                    bodyNodes[bodyNodes.indexOf(function)] = function.copy(body = newBody)
                }
            } else if (features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
                bodyNodes.add(
                    AssignNode(
                        target = NameNode(id = "arrayData", ctx = Store),
                        value = ListNode(elements = arrayElements, metadata = mapOf("arrayType" to "string")),
                        metadata = mapOf("variableType" to "string[]")
                    )
                )
            }
        }

        // Generate tuple literal assignment if requested
        if (features.contains(AstFeature.TUPLE_LITERALS)) {
            val tupleElements = if (features.contains(AstFeature.CONSTANT_VALUES)) {
                listOf(ConstantNode("name"), ConstantNode(42))  // Mixed types: string and number
            } else {
                listOf(
                    NameNode(id = "first", ctx = Load),
                    NameNode(id = "second", ctx = Load)
                )
            }

            // If in a function, add to function body; otherwise standalone
            if (features.contains(AstFeature.FUNCTION_DEFINITIONS) && bodyNodes.isNotEmpty()) {
                val function = bodyNodes.find { it is FunctionDefNode } as? FunctionDefNode
                if (function != null && features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
                    val newBody = function.body.toMutableList()
                    // Find the index of the return statement (if any)
                    val returnIndex = newBody.indexOfFirst { it is ReturnNode }
                    val insertIndex = if (returnIndex >= 0) returnIndex else newBody.size
                    // Insert before the return statement, or at the end if no return
                    newBody.add(
                        insertIndex,
                        AssignNode(
                            target = NameNode(id = "tupleData", ctx = Store),
                            value = TupleNode(
                                elements = tupleElements,
                                metadata = mapOf("tupleTypes" to listOf("string", "number"))
                            ),
                            metadata = mapOf("variableType" to "[string, number]")
                        )
                    )
                    bodyNodes[bodyNodes.indexOf(function)] = function.copy(body = newBody)
                }
            } else if (features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
                bodyNodes.add(
                    AssignNode(
                        target = NameNode(id = "tupleData", ctx = Store),
                        value = TupleNode(
                            elements = tupleElements,
                            metadata = mapOf("tupleTypes" to listOf("string", "number"))
                        ),
                        metadata = mapOf("variableType" to "[string, number]")
                    )
                )
            }
        }

        return ModuleNode(body = bodyNodes)
    }

    /**
     * Generates a simpler function AST with metadata (function only)
     */
    fun generateFunctionWithMetadata(): ModuleNode {
        return ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "test",
                    args = listOf(),
                    body = listOf(
                        AssignNode(
                            target = NameNode(id = "result", ctx = Store),
                            value = ConstantNode("hello"),
                            metadata = mapOf("variableType" to "string")
                        )
                    ),
                    decoratorList = emptyList(),
                    metadata = mapOf("returnType" to "void")
                )
            )
        )
    }

    /**
     * Generates a simple assignment AST with metadata (assignment only)
     */
    fun generateAssignmentWithMetadata(): ModuleNode {
        return ModuleNode(
            body = listOf(
                AssignNode(
                    target = NameNode(id = "result", ctx = Store),
                    value = ConstantNode("hello"),
                    metadata = mapOf("variableType" to "string")
                ),
                PrintNode(
                    expression = NameNode(id = "result", ctx = Load)
                )
            )
        )
    }

    /**
     * Generates an AST for a for-each loop.
     * Example: `for (item in collection) { print(item) }`
     */
    fun generateForEachLoop(): ForEachNode {
        val iterable = NameNode(id = "myCollection", ctx = Load, metadata = mapOf("type" to "List<number>"))
        val variable = NameNode(id = "item", ctx = Store, metadata = mapOf("type" to "number"))
        val body = listOf(
            PrintNode(
                expression = NameNode(id = "item", ctx = Load)
            )
        )
        return ForEachNode(
            iterable = iterable,
            variable = variable,
            body = body,
            metadata = mapOf(
                "loopType" to "forEach",
                "iterableType" to "List<number>",
                "itemType" to "number"
            )
        )
    }

    /**
     * Generates an AST for a for-each loop.
     * Example: `for (item in collection) { print(item) }`
     */
    fun generateForEachLoop(): ForEachNode {
        val iterable = NameNode(id = "myCollection", ctx = Load, metadata = mapOf("type" to "List<number>"))
        val variable = NameNode(id = "item", ctx = Store, metadata = mapOf("type" to "number"))
        val body = listOf(
            PrintNode(
                expression = NameNode(id = "item", ctx = Load)
            )
        )
        return ForEachNode(
            iterable = iterable,
            variable = variable,
            body = body,
            metadata = mapOf(
                "loopType" to "forEach",
                "iterableType" to "List<number>",
                "itemType" to "number"
            )
        )
    }

    /**
     * Generates an AST for a for-each loop.
     * Example: `for (item in collection) { print(item) }`
     */
    fun generateForEachLoop(): ForEachNode {
        val iterable = NameNode(id = "myCollection", ctx = Load, metadata = mapOf("type" to "List<number>"))
        val variable = NameNode(id = "item", ctx = Store, metadata = mapOf("type" to "number"))
        val body = listOf(
            PrintNode(
                expression = NameNode(id = "item", ctx = Load)
            )
        )
        return ForEachNode(
            iterable = iterable,
            variable = variable,
            body = body,
            metadata = mapOf(
                "loopType" to "forEach",
                "iterableType" to "List<number>",
                "itemType" to "number"
            )
        )
    }

    /**
     * Generates an AST for a for-each loop.
     * Example: `for (item in collection) { print(item) }`
     */
    fun generateForEachLoop(): ForEachNode {
        val iterable = NameNode(id = "myCollection", ctx = Load, metadata = mapOf("type" to "List<number>"))
        val variable = NameNode(id = "item", ctx = Store, metadata = mapOf("type" to "number"))
        val body = listOf(
            PrintNode(
                expression = NameNode(id = "item", ctx = Load)
            )
        )
        return ForEachNode(
            iterable = iterable,
            variable = variable,
            body = body,
            metadata = mapOf(
                "loopType" to "forEach",
                "iterableType" to "List<number>",
                "itemType" to "number"
            )
        )
    }

    /**
     * Generates a function with simple return statement (no value)
     */
    fun generateFunctionWithReturnStatement(): ModuleNode {
        return ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "test_return",
                    args = listOf(
                        NameNode(id = "input", ctx = Param, metadata = mapOf("type" to "string"))
                    ),
                    body = listOf(
                        ReturnNode(value = null)
                    ),
                    decoratorList = emptyList(),
                    metadata = mapOf(
                        "returnType" to "void",
                        "paramTypes" to mapOf("input" to "string")
                    )
                )
            )
        )
    }

    /**
     * Generates a function with return value (binary operation)
     */
    fun generateFunctionWithReturnValue(): ModuleNode {
        return ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "add",
                    args = listOf(
                        NameNode(id = "a", ctx = Param, metadata = mapOf("type" to "number")),
                        NameNode(id = "b", ctx = Param, metadata = mapOf("type" to "number"))
                    ),
                    body = listOf(
                        ReturnNode(
                            value = BinaryOpNode(
                                left = NameNode(id = "a", ctx = Load),
                                op = "+",
                                right = NameNode(id = "b", ctx = Load)
                            )
                        )
                    ),
                    decoratorList = emptyList(),
                    metadata = mapOf(
                        "returnType" to "number",
                        "paramTypes" to mapOf("a" to "number", "b" to "number")
                    )
                )
            )
        )
    }
}

data class LanguageConfig(
    val name: String,
    val parseWithMetadataFn: (String, List<LanguageMetadata>) -> AstNode,
    val generateWithMetadataFn: (AstNode) -> CodeWithMetadata
)




package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.ast.*

// --- Wrapper constructors for maximal metadata population in tests ---

/**
 * Creates an AssignNode with maximal metadata population for testing
 */
fun createMaximalAssignNode(
    targetId: String,
    value: ExpressionNode,
    variableType: CanonicalTypes = CanonicalTypes.Unknown,
    customType: String? = null
): AssignNode = AssignNode(
    target = NameNode(id = targetId, ctx = Store),
    value = value,
    variableType = variableType,
    customVariableType = customType
)

/**
 * Creates a ClassDefNode with maximal metadata population for testing
 */
fun createMaximalClassDefNode(
    name: String,
    body: List<StatementNode>,
    customClassType: String? = null,
    baseClasses: List<ExpressionNode> = emptyList()
): ClassDefNode = ClassDefNode(
    name = name,
    baseClasses = baseClasses,
    body = body,
    decoratorList = emptyList(),
    classType = CanonicalTypes.Any,
    customClassType = customClassType ?: name,
    methods = body.filterIsInstance<FunctionDefNode>().map { it.name }
)

/**
 * Creates a FunctionDefNode with maximal metadata population for testing
 */
fun createMaximalFunctionDefNode(
    name: String,
    args: List<NameNode> = emptyList(),
    body: List<StatementNode>,
    returnType: CanonicalTypes = CanonicalTypes.Void
): FunctionDefNode = FunctionDefNode(
    name = name,
    args = args,
    body = body,
    decoratorList = emptyList(),
    returnType = returnType,
    paramTypes = args.associate { it.id to it.type },
    individualParamMetadata = args.associate { it.id to mapOf("name" to it.id, "type" to it.type.name) }
)

/**
 * Creates a TupleNode with maximal metadata population for testing
 */
fun createMaximalTupleNode(
    elements: List<ExpressionNode>,
    tupleTypes: List<CanonicalTypes> = emptyList()
): TupleNode = TupleNode(
    elements = elements,
    tupleTypes = if (tupleTypes.isNotEmpty()) tupleTypes else 
        elements.map { 
            when (it) {
                is ConstantNode -> when (it.value) {
                    is String -> CanonicalTypes.String
                    is Number -> CanonicalTypes.Number
                    is Boolean -> CanonicalTypes.Boolean
                    else -> CanonicalTypes.Unknown
                }
                else -> CanonicalTypes.Unknown
            }
        }
)

/**
 * Creates a NameNode with maximal metadata population for testing
 */
fun createMaximalNameNode(
    id: String,
    ctx: NameContext,
    type: CanonicalTypes = CanonicalTypes.Unknown
): NameNode = NameNode(id = id, ctx = ctx, type = type)

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
    TUPLE_LITERALS
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
                        variableType = CanonicalTypes.String
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
                    NameNode(id = "input", ctx = Param, type = CanonicalTypes.String),
                    NameNode(id = "count", ctx = Param, type = CanonicalTypes.Number)
                )
            } else {
                emptyList()
            }

            // Determine return type based on features
            val returnType = if (features.contains(AstFeature.RETURN_STATEMENTS)) {
                if (features.contains(AstFeature.VARIABLE_ASSIGNMENTS) && features.contains(AstFeature.VARIABLE_REFERENCES)) {
                    CanonicalTypes.String // returning result variable
                } else if (features.contains(AstFeature.BINARY_OPERATIONS)) {
                    CanonicalTypes.Number // returning binary operation result
                } else if (features.contains(AstFeature.CONSTANT_VALUES)) {
                    CanonicalTypes.String // returning constant value
                } else {
                    CanonicalTypes.Void // returning null
                }
            } else {
                CanonicalTypes.Void
            }

            bodyNodes.add(
                FunctionDefNode(
                    name = "processData",
                    args = functionArgs,
                    body = functionBody,
                    decoratorList = emptyList(),
                    returnType = returnType,
                    paramTypes = mapOf("input" to CanonicalTypes.String, "count" to CanonicalTypes.Number)
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
                            variableType = CanonicalTypes.String
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
                            listOf(NameNode(id = "newValue", ctx = Param, type = CanonicalTypes.String))
                        } else {
                            emptyList()
                        },
                        body = methodBody,
                        decoratorList = emptyList(),
                        returnType = CanonicalTypes.String,
                        paramTypes = mapOf("newValue" to CanonicalTypes.String)
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
                        returnType = CanonicalTypes.Void
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
                        returnType = CanonicalTypes.Void
                    )
                )
            }

            bodyNodes.add(
                createMaximalClassDefNode(
                    name = "DataProcessor",
                    body = classBody,
                    customClassType = "DataProcessor"
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
                    variableType = CanonicalTypes.String
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
                            value = ListNode(elements = arrayElements, arrayType = CanonicalTypes.String),
                            variableType = CanonicalTypes.String
                        )
                    )
                    bodyNodes[bodyNodes.indexOf(function)] = function.copy(body = newBody)
                }
            } else if (features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
                bodyNodes.add(
                    AssignNode(
                        target = NameNode(id = "arrayData", ctx = Store),
                        value = ListNode(elements = arrayElements, arrayType = CanonicalTypes.String),
                        variableType = CanonicalTypes.String
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
                        createMaximalAssignNode(
                            targetId = "tupleData",
                            value = createMaximalTupleNode(
                                elements = tupleElements,
                                tupleTypes = listOf(CanonicalTypes.String, CanonicalTypes.Number)
                            ),
                            variableType = CanonicalTypes.Any,
                            customType = "[string, number]"
                        )
                    )
                    bodyNodes[bodyNodes.indexOf(function)] = function.copy(body = newBody)
                }
            } else if (features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
                bodyNodes.add(
                    createMaximalAssignNode(
                        targetId = "tupleData",
                        value = createMaximalTupleNode(
                            elements = tupleElements,
                            tupleTypes = listOf(CanonicalTypes.String, CanonicalTypes.Number)
                        ),
                        variableType = CanonicalTypes.Any,
                        customType = "[string, number]"
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
                            variableType = CanonicalTypes.String
                        )
                    ),
                    decoratorList = emptyList(),
                    returnType = CanonicalTypes.Void
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
                    variableType = CanonicalTypes.String
                ),
                PrintNode(
                    expression = NameNode(id = "result", ctx = Load)
                )
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
                        NameNode(id = "input", ctx = Param, type = CanonicalTypes.String)
                    ),
                    body = listOf(
                        ReturnNode(value = null)
                    ),
                    decoratorList = emptyList(),
                    returnType = CanonicalTypes.Void,
                    paramTypes = mapOf("input" to CanonicalTypes.String)
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
                        NameNode(id = "a", ctx = Param, type = CanonicalTypes.Number),
                        NameNode(id = "b", ctx = Param, type = CanonicalTypes.Number)
                    ),
                    body = listOf(
                        ReturnNode(
                            value = BinaryOpNode(
                                left = NameNode(id = "a", ctx = Load),
                                op = "+",
                                right = NameNode(id = "b", ctx = Load),
                                resultType = CanonicalTypes.Number
                            )
                        )
                    ),
                    decoratorList = emptyList(),
                    returnType = CanonicalTypes.Number,
                    paramTypes = mapOf("a" to CanonicalTypes.Number, "b" to CanonicalTypes.Number)
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
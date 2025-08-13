package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.ast.*

// --- Wrapper constructors for maximal metadata population in tests ---

/**
 * Creates a ModuleNode with maximal metadata population for testing
 */
fun createMaximalModuleNode(
    body: List<StatementNode>
): ModuleNode = ModuleNode(body = body)

/**
 * Creates an AssignNode with maximal metadata population for testing
 */
fun createMaximalAssignNode(
    targetId: String,
    value: ExpressionNode,
    typeInfo: TypeInfo = CanonicalTypes.Unknown
): AssignNode = AssignNode(
    target = NameNode(id = targetId, ctx = Store),
    value = value,
    typeInfo = typeInfo
)

/**
 * Creates an AssignNode with TypeDefinition for enhanced type safety
 */
fun createMaximalAssignNodeWithType(
    targetId: String,
    value: ExpressionNode,
    typeDefinition: TypeDefinition
): AssignNode = AssignNode(
    target = NameNode(id = targetId, ctx = Store),
    value = value,
    typeInfo = typeDefinition
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
    typeInfo = if (customClassType != null) TypeDefinition.custom(customClassType) else CanonicalTypes.Any,
    methods = body.filterIsInstance<FunctionDefNode>().map { it.name }
)

/**
 * Creates a ClassDefNode with TypeDefinition for enhanced type safety
 */
fun createMaximalClassDefNodeWithType(
    name: String,
    body: List<StatementNode>,
    typeDefinition: TypeDefinition = TypeDefinition.custom(name),
    baseClasses: List<ExpressionNode> = emptyList()
): ClassDefNode = ClassDefNode(
    name = name,
    baseClasses = baseClasses,
    body = body,
    decoratorList = emptyList(),
    typeInfo = typeDefinition,
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
    paramTypes = args.associate { it.id to it.typeInfo },
    individualParamMetadata = emptyMap() // Don't populate this for round-trip compatibility
)

/**
 * Creates a TupleNode with maximal metadata population for testing
 */
fun createMaximalTupleNode(
    elements: List<ExpressionNode>,
    tupleTypes: List<CanonicalTypes> = emptyList()
): TupleNode = TupleNode(
    elements = elements,
    typeInfo = if (tupleTypes.isNotEmpty()) TypeDefinition.Tuple(tupleTypes) else 
        TypeDefinition.Tuple(elements.map { 
            when (it) {
                is ConstantNode -> when (it.value) {
                    is String -> CanonicalTypes.String
                    is Number -> CanonicalTypes.Number
                    is Boolean -> CanonicalTypes.Boolean
                    else -> CanonicalTypes.Unknown
                }
                else -> CanonicalTypes.Unknown
            }
        })
)

/**
 * Creates a NameNode with maximal metadata population for testing
 */
fun createMaximalNameNode(
    id: String,
    ctx: NameContext,
    type: CanonicalTypes = CanonicalTypes.Unknown
): NameNode = NameNode(id = id, ctx = ctx, typeInfo = type)

/**
 * Creates a BinaryOpNode with maximal metadata population for testing
 */
fun createMaximalBinaryOpNode(
    left: ExpressionNode,
    op: String,
    right: ExpressionNode,
    resultType: CanonicalTypes = CanonicalTypes.Unknown
): BinaryOpNode = BinaryOpNode(
    left = left,
    op = op,
    right = right,
    typeInfo = resultType
)

/**
 * Creates a ConstantNode with maximal metadata population for testing
 */
fun createMaximalConstantNode(
    value: Any?,
    constantType: CanonicalTypes = CanonicalTypes.Unknown
): ConstantNode = ConstantNode(
    value = value,
    typeInfo = constantType
)

/**
 * Creates a ListNode with maximal metadata population for testing
 */
fun createMaximalListNode(
    elements: List<ExpressionNode>,
    arrayType: CanonicalTypes = CanonicalTypes.Unknown,
    isHomogeneous: Boolean = true
): ListNode = ListNode(
    elements = elements,
    typeInfo = arrayType,
    isHomogeneous = isHomogeneous
)

/**
 * Creates a CompareNode with maximal metadata population for testing
 */
fun createMaximalCompareNode(
    left: ExpressionNode,
    op: String,
    right: ExpressionNode
): CompareNode = CompareNode(
    left = left,
    op = op,
    right = right
)

/**
 * Creates a CallNode with maximal metadata population for testing
 */
fun createMaximalCallNode(
    func: ExpressionNode,
    args: List<ExpressionNode>,
    keywords: List<Any> = emptyList()
): CallNode = CallNode(
    func = func,
    args = args,
    keywords = keywords
)

/**
 * Creates a CallStatementNode with maximal metadata population for testing
 */
fun createMaximalCallStatementNode(
    call: CallNode
): CallStatementNode = CallStatementNode(call = call)

/**
 * Creates a PrintNode with maximal metadata population for testing
 */
fun createMaximalPrintNode(
    expression: ExpressionNode
): PrintNode = PrintNode(expression = expression)

/**
 * Creates a ReturnNode with maximal metadata population for testing
 */
fun createMaximalReturnNode(
    value: ExpressionNode? = null
): ReturnNode = ReturnNode(value = value)

/**
 * Creates an IfNode with maximal metadata population for testing
 */
fun createMaximalIfNode(
    test: ExpressionNode,
    body: List<StatementNode>,
    orelse: List<StatementNode> = emptyList()
): IfNode = IfNode(
    test = test,
    body = body,
    orelse = orelse
)

/**
 * Creates a MemberExpressionNode with maximal metadata population for testing
 */
fun createMaximalMemberExpressionNode(
    obj: ExpressionNode,
    property: ExpressionNode
): MemberExpressionNode = MemberExpressionNode(
    obj = obj,
    property = property
)

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
                    createMaximalBinaryOpNode(
                        left = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                            createMaximalNameNode(id = "input", ctx = Load, type = CanonicalTypes.String) 
                            else createMaximalConstantNode("input", CanonicalTypes.String),
                        op = "+",
                        right = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                            createMaximalNameNode(id = "count", ctx = Load, type = CanonicalTypes.Number) 
                            else createMaximalConstantNode(1, CanonicalTypes.Number),
                        resultType = CanonicalTypes.String
                    )
                } else if (features.contains(AstFeature.CONSTANT_VALUES)) {
                    createMaximalConstantNode("hello", CanonicalTypes.String)
                } else {
                    createMaximalNameNode(id = "input", ctx = Load, type = CanonicalTypes.String)
                }

                functionBody.add(
                    createMaximalAssignNode(
                        targetId = "result",
                        value = assignValue,
                        typeInfo = CanonicalTypes.String
                    )
                )
            }

            // Add print statement if requested
            if (features.contains(AstFeature.PRINT_STATEMENTS)) {
                functionBody.add(
                    createMaximalPrintNode(
                        expression = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                            createMaximalNameNode(id = "result", ctx = Load, type = CanonicalTypes.String) 
                            else createMaximalConstantNode("output", CanonicalTypes.String)
                    )
                )
            }

            // Add return statement if requested
            if (features.contains(AstFeature.RETURN_STATEMENTS)) {
                val returnValue =
                    if (features.contains(AstFeature.VARIABLE_REFERENCES) && features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
                        // Return the result variable if we have assignments and variable references
                        createMaximalNameNode(id = "result", ctx = Load, type = CanonicalTypes.String)
                    } else if (features.contains(AstFeature.BINARY_OPERATIONS)) {
                        // Return a binary operation
                        createMaximalBinaryOpNode(
                            left = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                                createMaximalNameNode(id = "input", ctx = Load, type = CanonicalTypes.String) 
                                else createMaximalConstantNode("a", CanonicalTypes.String),
                            op = "+",
                            right = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                                createMaximalNameNode(id = "count", ctx = Load, type = CanonicalTypes.Number) 
                                else createMaximalConstantNode(1, CanonicalTypes.Number),
                            resultType = CanonicalTypes.String
                        )
                    } else if (features.contains(AstFeature.CONSTANT_VALUES)) {
                        // Return a constant
                        createMaximalConstantNode("returned_value", CanonicalTypes.String)
                    } else {
                        // Return null (void return)
                        null
                    }

                functionBody.add(createMaximalReturnNode(value = returnValue))
            }

            // Create function arguments based on features
            val functionArgs = if (features.contains(AstFeature.VARIABLE_REFERENCES)) {
                listOf(
                    createMaximalNameNode(id = "input", ctx = Param, type = CanonicalTypes.String),
                    createMaximalNameNode(id = "count", ctx = Param, type = CanonicalTypes.Number)
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
                createMaximalFunctionDefNode(
                    name = "processData",
                    args = functionArgs,
                    body = functionBody,
                    returnType = returnType
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
                        createMaximalAssignNode(
                            targetId = "instanceValue",
                            value = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                                createMaximalNameNode(id = "newValue", ctx = Load, type = CanonicalTypes.String) 
                                else createMaximalConstantNode("initialized", CanonicalTypes.String),
                            typeInfo = CanonicalTypes.String
                        )
                    )
                }

                if (features.contains(AstFeature.RETURN_STATEMENTS)) {
                    methodBody.add(
                        createMaximalReturnNode(
                            value = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                                createMaximalNameNode(id = "instanceValue", ctx = Load, type = CanonicalTypes.String) 
                                else createMaximalConstantNode("method_result", CanonicalTypes.String)
                        )
                    )
                }

                classBody.add(
                    createMaximalFunctionDefNode(
                        name = "getValue",
                        args = if (features.contains(AstFeature.VARIABLE_REFERENCES)) {
                            listOf(createMaximalNameNode(id = "newValue", ctx = Param, type = CanonicalTypes.String))
                        } else {
                            emptyList()
                        },
                        body = methodBody,
                        returnType = CanonicalTypes.String
                    )
                )
            }

            // Add a simple method even if function definitions are not requested
            if (!features.contains(AstFeature.FUNCTION_DEFINITIONS) && features.contains(AstFeature.PRINT_STATEMENTS)) {
                classBody.add(
                    createMaximalFunctionDefNode(
                        name = "display",
                        args = emptyList(),
                        body = listOf(
                            createMaximalPrintNode(
                                expression = if (features.contains(AstFeature.CONSTANT_VALUES))
                                    createMaximalConstantNode("Class instance", CanonicalTypes.String) 
                                    else createMaximalConstantNode("Display method", CanonicalTypes.String)
                            )
                        ),
                        returnType = CanonicalTypes.Void
                    )
                )
            }

            // Always ensure class has at least one method to avoid empty class bodies
            if (classBody.isEmpty()) {
                classBody.add(
                    createMaximalFunctionDefNode(
                        name = "defaultMethod",
                        args = emptyList(),
                        body = listOf(
                            createMaximalPrintNode(expression = createMaximalConstantNode("Default class method", CanonicalTypes.String))
                        ),
                        returnType = CanonicalTypes.Void
                    )
                )
            }

            bodyNodes.add(
                createMaximalClassDefNodeWithType(
                    name = "DataProcessor",
                    body = classBody,
                    typeDefinition = TypeDefinition.custom("DataProcessor")
                )
            )
        }

        // Generate standalone variable assignment if requested and not already in function
        if (features.contains(AstFeature.VARIABLE_ASSIGNMENTS) && !features.contains(AstFeature.FUNCTION_DEFINITIONS)) {
            val assignValue = if (features.contains(AstFeature.CONSTANT_VALUES)) {
                createMaximalConstantNode("standalone", CanonicalTypes.String)
            } else {
                createMaximalNameNode(id = "defaultValue", ctx = Load, type = CanonicalTypes.String)
            }

            bodyNodes.add(
                createMaximalAssignNode(
                    targetId = "standalone",
                    value = assignValue,
                    typeInfo = CanonicalTypes.String
                )
            )
        }

        // Generate function call statement if requested
        if (features.contains(AstFeature.FUNCTION_CALLS)) {
            val callArgs = if (features.contains(AstFeature.CONSTANT_VALUES)) {
                listOf(
                    createMaximalConstantNode("hello", CanonicalTypes.String), 
                    createMaximalConstantNode(42, CanonicalTypes.Number)
                )
            } else {
                emptyList()
            }

            bodyNodes.add(
                createMaximalCallStatementNode(
                    call = createMaximalCallNode(
                        func = createMaximalNameNode(id = "processData", ctx = Load, type = CanonicalTypes.Unknown),
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
                createMaximalCompareNode(
                    left = if (features.contains(AstFeature.VARIABLE_REFERENCES))
                        createMaximalNameNode(id = "x", ctx = Load, type = CanonicalTypes.Number) 
                        else createMaximalConstantNode(10, CanonicalTypes.Number),
                    op = ">",
                    right = if (features.contains(AstFeature.CONSTANT_VALUES))
                        createMaximalConstantNode(5, CanonicalTypes.Number) 
                        else createMaximalConstantNode(0, CanonicalTypes.Number)
                )
            } else {
                // Simple boolean variable or constant
                if (features.contains(AstFeature.VARIABLE_REFERENCES))
                    createMaximalNameNode(id = "condition", ctx = Load, type = CanonicalTypes.Boolean) 
                    else createMaximalConstantNode(true, CanonicalTypes.Boolean)
            }

            // Simple if-else with print statements
            bodyNodes.add(
                createMaximalIfNode(
                    test = testCondition,
                    body = listOf(
                        createMaximalPrintNode(
                            expression = if (features.contains(AstFeature.CONSTANT_VALUES))
                                createMaximalConstantNode("condition is true", CanonicalTypes.String) 
                                else createMaximalNameNode(id = "trueMessage", ctx = Load, type = CanonicalTypes.String)
                        )
                    ),
                    orelse = listOf(
                        createMaximalPrintNode(
                            expression = if (features.contains(AstFeature.CONSTANT_VALUES))
                                createMaximalConstantNode("condition is false", CanonicalTypes.String) 
                                else createMaximalNameNode(id = "falseMessage", ctx = Load, type = CanonicalTypes.String)
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
                createMaximalPrintNode(
                    expression = if (features.contains(AstFeature.CONSTANT_VALUES))
                        createMaximalConstantNode("standalone print", CanonicalTypes.String) 
                        else createMaximalNameNode(id = "printValue", ctx = Load, type = CanonicalTypes.String)
                )
            )
        }

        // Generate array literal assignment if requested
        if (features.contains(AstFeature.ARRAY_LITERALS)) {
            val arrayElements = if (features.contains(AstFeature.CONSTANT_VALUES)) {
                listOf(
                    createMaximalConstantNode("item1", CanonicalTypes.String), 
                    createMaximalConstantNode("item2", CanonicalTypes.String), 
                    createMaximalConstantNode("item3", CanonicalTypes.String)
                )
            } else {
                listOf(
                    createMaximalNameNode(id = "elem1", ctx = Load, type = CanonicalTypes.String),
                    createMaximalNameNode(id = "elem2", ctx = Load, type = CanonicalTypes.String),
                    createMaximalNameNode(id = "elem3", ctx = Load, type = CanonicalTypes.String)
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
                            targetId = "arrayData",
                            value = createMaximalListNode(
                                elements = arrayElements, 
                                arrayType = CanonicalTypes.String,
                                isHomogeneous = true
                            ),
                            typeInfo = CanonicalTypes.String
                        )
                    )
                    bodyNodes[bodyNodes.indexOf(function)] = function.copy(body = newBody)
                }
            } else if (features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
                bodyNodes.add(
                    createMaximalAssignNode(
                        targetId = "arrayData",
                        value = createMaximalListNode(
                            elements = arrayElements, 
                            arrayType = CanonicalTypes.String,
                            isHomogeneous = true
                        ),
                        typeInfo = CanonicalTypes.String
                    )
                )
            }
        }

        // Generate tuple literal assignment if requested
        if (features.contains(AstFeature.TUPLE_LITERALS)) {
            val tupleElements = if (features.contains(AstFeature.CONSTANT_VALUES)) {
                listOf(
                    createMaximalConstantNode("name", CanonicalTypes.String), 
                    createMaximalConstantNode(42, CanonicalTypes.Number)
                )  // Mixed types: string and number
            } else {
                listOf(
                    createMaximalNameNode(id = "first", ctx = Load, type = CanonicalTypes.String),
                    createMaximalNameNode(id = "second", ctx = Load, type = CanonicalTypes.Number)
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
                        createMaximalAssignNodeWithType(
                            targetId = "tupleData",
                            value = createMaximalTupleNode(
                                elements = tupleElements,
                                tupleTypes = listOf(CanonicalTypes.String, CanonicalTypes.Number)
                            ),
                            typeDefinition = TypeDefinition.tuple(CanonicalTypes.String, CanonicalTypes.Number)
                        )
                    )
                    bodyNodes[bodyNodes.indexOf(function)] = function.copy(body = newBody)
                }
            } else if (features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
                bodyNodes.add(
                    createMaximalAssignNodeWithType(
                        targetId = "tupleData",
                        value = createMaximalTupleNode(
                            elements = tupleElements,
                            tupleTypes = listOf(CanonicalTypes.String, CanonicalTypes.Number)
                        ),
                        typeDefinition = TypeDefinition.tuple(CanonicalTypes.String, CanonicalTypes.Number)
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
                createMaximalFunctionDefNode(
                    name = "test",
                    args = listOf(),
                    body = listOf(
                        createMaximalAssignNode(
                            targetId = "result",
                            value = createMaximalConstantNode("hello", CanonicalTypes.String),
                            typeInfo = CanonicalTypes.String
                        )
                    ),
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
                createMaximalAssignNode(
                    targetId = "result",
                    value = createMaximalConstantNode("hello", CanonicalTypes.String),
                    typeInfo = CanonicalTypes.String
                ),
                createMaximalPrintNode(
                    expression = createMaximalNameNode(id = "result", ctx = Load, type = CanonicalTypes.String)
                )
            )
        )
    }

    /**
     * Generates a function with simple return statement (no value)
     */
    fun generateFunctionWithReturnStatement(): ModuleNode {
        return createMaximalModuleNode(
            body = listOf(
                createMaximalFunctionDefNode(
                    name = "test_return",
                    args = listOf(
                        createMaximalNameNode(id = "input", ctx = Param, type = CanonicalTypes.String)
                    ),
                    body = listOf(
                        createMaximalReturnNode(value = null)
                    ),
                    returnType = CanonicalTypes.Void
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
                createMaximalFunctionDefNode(
                    name = "add",
                    args = listOf(
                        createMaximalNameNode(id = "a", ctx = Param, type = CanonicalTypes.Number),
                        createMaximalNameNode(id = "b", ctx = Param, type = CanonicalTypes.Number)
                    ),
                    body = listOf(
                        createMaximalReturnNode(
                            value = createMaximalBinaryOpNode(
                                left = createMaximalNameNode(id = "a", ctx = Load, type = CanonicalTypes.Number),
                                op = "+",
                                right = createMaximalNameNode(id = "b", ctx = Load, type = CanonicalTypes.Number),
                                resultType = CanonicalTypes.Number
                            )
                        )
                    ),
                    returnType = CanonicalTypes.Number
                )
            )
        )
    }
}

data class LanguageConfig(
    val name: String,
    val parseWithNativeMetadataFn: (String, List<LanguageMetadata>) -> AstNode,
    val generateWithNativeMetadataFn: (AstNode) -> CodeWithNativeMetadata
)
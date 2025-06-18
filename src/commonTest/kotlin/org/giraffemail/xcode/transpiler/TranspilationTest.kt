package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.javaparser.JavaGenerator
import org.giraffemail.xcode.javaparser.JavaParser
import org.giraffemail.xcode.javascriptparser.JavaScriptGenerator
import org.giraffemail.xcode.javascriptparser.JavaScriptParser
import org.giraffemail.xcode.pythonparser.PythonGenerator
import org.giraffemail.xcode.pythonparser.PythonParser
import org.giraffemail.xcode.typescriptparser.TypeScriptGenerator
import org.giraffemail.xcode.typescriptparser.TypeScriptParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

data class LanguageConfig(
    val name: String,
    val parseWithMetadataFn: (String, List<LanguageMetadata>) -> AstNode,
    val generateWithMetadataFn: (AstNode) -> CodeWithMetadata
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
    TUPLE_LITERALS,
    BOOLEAN_LITERALS
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
        AstFeature.TUPLE_LITERALS,
        AstFeature.BOOLEAN_LITERALS
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
        "Array literals with typed elements",
        "Tuple literals with mixed types",
        "Boolean literals (true, false)"
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
                val returnValue = if (features.contains(AstFeature.VARIABLE_REFERENCES) && features.contains(AstFeature.VARIABLE_ASSIGNMENTS)) {
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
            !features.contains(AstFeature.CONDITIONAL_STATEMENTS)) {
            bodyNodes.add(
                PrintNode(
                    expression = if (features.contains(AstFeature.CONSTANT_VALUES))
                        ConstantNode("standalone print") else NameNode(id = "printValue", ctx = Load)
                )
            )
        }
        
        // Generate array literal assignments if requested
        if (features.contains(AstFeature.ARRAY_LITERALS)) {
            bodyNodes.add(
                AssignNode(
                    target = NameNode(id = "arrayVar", ctx = Store),
                    value = ListNode(
                        elements = if (features.contains(AstFeature.CONSTANT_VALUES)) {
                            listOf(ConstantNode("item1"), ConstantNode("item2"), ConstantNode("item3"))
                        } else {
                            listOf(NameNode(id = "a", ctx = Load), NameNode(id = "b", ctx = Load))
                        }
                    ),
                    metadata = mapOf("variableType" to "string[]")
                )
            )
        }
        
        // Generate tuple literal assignments if requested
        if (features.contains(AstFeature.TUPLE_LITERALS)) {
            bodyNodes.add(
                AssignNode(
                    target = NameNode(id = "tupleVar", ctx = Store),
                    value = ListNode(
                        elements = if (features.contains(AstFeature.CONSTANT_VALUES)) {
                            listOf(ConstantNode("name"), ConstantNode(25))
                        } else {
                            listOf(NameNode(id = "firstName", ctx = Load), NameNode(id = "age", ctx = Load))
                        },
                        metadata = mapOf("tupleType" to "[string, number]")
                    ),
                    metadata = mapOf("variableType" to "[string, number]")
                )
            )
        }
        
        // Generate boolean literal assignments if requested
        if (features.contains(AstFeature.BOOLEAN_LITERALS)) {
            bodyNodes.add(
                AssignNode(
                    target = NameNode(id = "boolVar1", ctx = Store),
                    value = ConstantNode(true),
                    metadata = mapOf("variableType" to "boolean")
                )
            )
            bodyNodes.add(
                AssignNode(
                    target = NameNode(id = "boolVar2", ctx = Store),
                    value = ConstantNode(false),
                    metadata = mapOf("variableType" to "boolean")
                )
            )
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

/**
 * Unit tests for the MaximalAstGenerator utility
 */
class MaximalAstGeneratorTest {

    @Test
    fun `test generateMaximalAst with all features generates expected structure`() {
        val ast = MaximalAstGenerator.generateMaximalAst()
        
        assertTrue(ast is ModuleNode)
        assertTrue(ast.body.isNotEmpty())
        
        // Should contain function definition, function call
        val functionDef = ast.body.find { it is FunctionDefNode }
        assertTrue(functionDef != null, "Should contain function definition")
        
        val functionCall = ast.body.find { it is CallStatementNode }
        assertTrue(functionCall != null, "Should contain function call")
    }

    @Test
    fun `test generateMaximalAst with only function definitions`() {
        val features = setOf(AstFeature.FUNCTION_DEFINITIONS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is FunctionDefNode)
        
        val function = ast.body[0] as FunctionDefNode
        assertEquals("processData", function.name)
    }

    @Test
    fun `test generateMaximalAst with only variable assignments`() {
        val features = setOf(AstFeature.VARIABLE_ASSIGNMENTS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is AssignNode)
        
        val assignment = ast.body[0] as AssignNode
        assertTrue(assignment.target is NameNode)
        assertEquals("standalone", (assignment.target as NameNode).id)
    }

    @Test
    fun `test generateMaximalAst with only print statements`() {
        val features = setOf(AstFeature.PRINT_STATEMENTS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is PrintNode)
    }

    @Test
    fun `test generateMaximalAst with only function calls`() {
        val features = setOf(AstFeature.FUNCTION_CALLS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is CallStatementNode)
        
        val call = ast.body[0] as CallStatementNode
        assertTrue(call.call.func is NameNode)
        assertEquals("processData", (call.call.func as NameNode).id)
    }

    @Test
    fun `test generateMaximalAst with function and assignment features`() {
        val features = setOf(AstFeature.FUNCTION_DEFINITIONS, AstFeature.VARIABLE_ASSIGNMENTS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is FunctionDefNode)
        
        val function = ast.body[0] as FunctionDefNode
        assertTrue(function.body.any { it is AssignNode })
    }

    @Test
    fun `test generateMaximalAst with constant values feature affects assignment`() {
        val featuresWithConstants = setOf(AstFeature.VARIABLE_ASSIGNMENTS, AstFeature.CONSTANT_VALUES)
        val ast = MaximalAstGenerator.generateMaximalAst(featuresWithConstants)
        
        val assignment = ast.body[0] as AssignNode
        assertTrue(assignment.value is ConstantNode)
        assertEquals("standalone", (assignment.value as ConstantNode).value)
    }

    @Test
    fun `test generateMaximalAst with binary operations feature`() {
        val features = setOf(
            AstFeature.FUNCTION_DEFINITIONS, 
            AstFeature.VARIABLE_ASSIGNMENTS, 
            AstFeature.BINARY_OPERATIONS,
            AstFeature.VARIABLE_REFERENCES
        )
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        val function = ast.body[0] as FunctionDefNode
        val assignment = function.body.find { it is AssignNode } as AssignNode
        assertTrue(assignment.value is BinaryOpNode)
        
        val binaryOp = assignment.value as BinaryOpNode
        assertEquals("+", binaryOp.op)
    }

    @Test
    fun `test generateMaximalAst with empty features set`() {
        val features = emptySet<AstFeature>()
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertTrue(ast.body.isEmpty())
    }

    @Test
    fun `test generateMaximalAst with only conditional statements`() {
        val features = setOf(AstFeature.CONDITIONAL_STATEMENTS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is IfNode)
        
        val ifNode = ast.body[0] as IfNode
        assertTrue(ifNode.body.isNotEmpty())
        assertTrue(ifNode.orelse.isNotEmpty())
    }

    @Test
    fun `test generateMaximalAst with conditional statements and comparisons`() {
        val features = setOf(AstFeature.CONDITIONAL_STATEMENTS, AstFeature.COMPARISON_OPERATIONS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is IfNode)
        
        val ifNode = ast.body[0] as IfNode
        assertTrue(ifNode.test is CompareNode)
    }

    @Test
    fun `test generateMaximalAst with conditional statements and constant values`() {
        val features = setOf(AstFeature.CONDITIONAL_STATEMENTS, AstFeature.CONSTANT_VALUES)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertTrue(ast.body[0] is IfNode)
        
        val ifNode = ast.body[0] as IfNode
        // Should use constant values in the condition and print statements
        assertTrue(ifNode.body.any { statement ->
            statement is PrintNode && (statement.expression as? ConstantNode)?.value == "condition is true"
        })
    }

    @Test
    fun `test generateMaximalAst with only class definitions`() {
        val features = setOf(AstFeature.CLASS_DEFINITIONS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is ClassDefNode)
        
        val classDef = ast.body[0] as ClassDefNode
        assertEquals("DataProcessor", classDef.name)
        assertEquals(emptyList<ExpressionNode>(), classDef.baseClasses)
    }

    @Test
    fun `test generateMaximalAst with class and function definitions`() {
        val features = setOf(AstFeature.CLASS_DEFINITIONS, AstFeature.FUNCTION_DEFINITIONS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertTrue(ast.body.any { it is ClassDefNode })
        assertTrue(ast.body.any { it is FunctionDefNode })
        
        val classDef = ast.body.find { it is ClassDefNode } as ClassDefNode
        assertTrue(classDef.body.any { it is FunctionDefNode })
        
        val classMethod = classDef.body.find { it is FunctionDefNode } as FunctionDefNode
        assertEquals("getValue", classMethod.name)
    }

    @Test
    fun `test generateMaximalAst with class definitions and other features`() {
        val features = setOf(
            AstFeature.CLASS_DEFINITIONS, 
            AstFeature.FUNCTION_DEFINITIONS,
            AstFeature.VARIABLE_ASSIGNMENTS,
            AstFeature.RETURN_STATEMENTS
        )
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        val classDef = ast.body.find { it is ClassDefNode } as ClassDefNode
        val classMethod = classDef.body.find { it is FunctionDefNode } as FunctionDefNode
        
        // Should have both assignment and return in the method
        assertTrue(classMethod.body.any { it is AssignNode })
        assertTrue(classMethod.body.any { it is ReturnNode })
    }
}

/**
 * Test suite for verifying AST preservation through transpilation chains.
 * Tests that ASTs with maximal metadata can round-trip through all supported languages
 * and preserve their metadata and structure.
 */
class TranspilationTest {

    private val pythonConfig = LanguageConfig(
        "Python", 
        PythonParser::parseWithMetadata,
        { ast -> PythonGenerator().generateWithMetadata(ast) }
    )
    private val javaScriptConfig = LanguageConfig(
        "JavaScript", 
        JavaScriptParser::parseWithMetadata,
        { ast -> JavaScriptGenerator().generateWithMetadata(ast) }
    )
    private val javaConfig = LanguageConfig(
        "Java", 
        JavaParser::parseWithMetadata,
        { ast -> JavaGenerator().generateWithMetadata(ast) }
    )
    private val typeScriptConfig = LanguageConfig(
        "TypeScript", 
        TypeScriptParser::parseWithMetadata,
        { ast -> TypeScriptGenerator().generateWithMetadata(ast) }
    )

    private val allLanguages = listOf(pythonConfig, javaScriptConfig, javaConfig, typeScriptConfig)

    /**
     * Test round-trip transpilation for a given AST through all language pairs.
     * Verifies that AST structure and metadata are preserved through all languages
     * using the parts-based metadata system.
     */
    private fun testAstRoundTrip(testName: String, originalAst: AstNode) {
        println("\\n=== Testing AST Round-Trip for '$testName' ===")
        println("Original AST: $originalAst")

        for (fromLang in allLanguages) {
            for (toLang in allLanguages) {
                if (fromLang == toLang) continue

                try {
                    println("\\nTesting ${fromLang.name} -> ${toLang.name} -> ${fromLang.name}")

                    // Step 1: Generate code with metadata from original AST using source language
                    val sourceCodeWithMetadata = fromLang.generateWithMetadataFn(originalAst)
                    println("Generated ${fromLang.name} code: ${sourceCodeWithMetadata.code}")
                    println("Generated ${fromLang.name} metadata: ${sourceCodeWithMetadata.metadata}")

                    // Step 2: Parse back to AST to verify generation didn't lose information
                    val parsedFromSource = fromLang.parseWithMetadataFn(sourceCodeWithMetadata.code, sourceCodeWithMetadata.metadata)
                    
                    // All languages should preserve metadata through parts-based system
                    assertEquals(
                        originalAst, parsedFromSource,
                        "AST changed after ${fromLang.name} generation/parsing round-trip"
                    )

                    // Step 3: Generate intermediate code with metadata using target language
                    val intermediateCodeWithMetadata = toLang.generateWithMetadataFn(parsedFromSource)
                    println("Generated ${toLang.name} code: ${intermediateCodeWithMetadata.code}")
                    println("Generated ${toLang.name} metadata: ${intermediateCodeWithMetadata.metadata}")

                    // Step 4: Parse intermediate code back to AST with metadata
                    val parsedFromTarget = toLang.parseWithMetadataFn(intermediateCodeWithMetadata.code, intermediateCodeWithMetadata.metadata)

                    // Step 5: Generate final code with metadata back to source language
                    val finalCodeWithMetadata = fromLang.generateWithMetadataFn(parsedFromTarget)
                    println("Final ${fromLang.name} code: ${finalCodeWithMetadata.code}")
                    println("Final ${fromLang.name} metadata: ${finalCodeWithMetadata.metadata}")

                    // Step 6: Parse final code and verify AST preservation
                    val finalAst = fromLang.parseWithMetadataFn(finalCodeWithMetadata.code, finalCodeWithMetadata.metadata)
                    
                    // Compare AST preservation through transpilation
                    // All languages should preserve metadata through parts-based system
                    assertEquals(
                        originalAst, finalAst,
                        "AST not preserved in ${fromLang.name} -> ${toLang.name} -> ${fromLang.name} round-trip"
                    )

                    println("✓ Round-trip successful")

                } catch (e: Exception) {
                    fail("Round-trip ${fromLang.name} -> ${toLang.name} -> ${fromLang.name} failed for '$testName': ${e.message}")
                }
            }
        }
        println("\\n=== '$testName' Round-Trip Testing Complete ===")
    }

    /**
     * Test sequential transpilation through all languages in a chain.
     * Verifies that AST structure and metadata are preserved through the entire sequence
     * using the parts-based metadata system.
     */
    private fun testSequentialTranspilation(testName: String, originalAst: AstNode) {
        println("\\n=== Testing Sequential Transpilation for '$testName' ===")
        println("Original AST: $originalAst")

        // Test all possible starting points
        for (startLangIndex in allLanguages.indices) {
            val sequence = mutableListOf<LanguageConfig>()
            
            // Create a sequence that visits every language once, starting from startLangIndex
            for (i in allLanguages.indices) {
                sequence.add(allLanguages[(startLangIndex + i) % allLanguages.size])
            }

            val sequenceNames = sequence.joinToString(" -> ") { it.name }
            println("\\nTesting sequence: $sequenceNames -> ${sequence.first().name}")

            try {
                var currentAst = originalAst

                // Go through the sequence
                for (i in 0 until sequence.size) {
                    val currentLang = sequence[i]
                    val nextLang = if (i < sequence.size - 1) sequence[i + 1] else sequence[0]

                    println("Step ${i + 1}: ${currentLang.name} -> ${nextLang.name}")
                    
                    // Generate code with metadata in current language
                    val codeWithMetadata = currentLang.generateWithMetadataFn(currentAst)
                    println("Generated ${currentLang.name} code: ${codeWithMetadata.code}")
                    println("Generated ${currentLang.name} metadata: ${codeWithMetadata.metadata}")

                    // Parse to verify AST preservation
                    val parsedAst = currentLang.parseWithMetadataFn(codeWithMetadata.code, codeWithMetadata.metadata)
                    
                    // All languages should preserve metadata through parts-based system
                    assertEquals(
                        currentAst, parsedAst,
                        "AST changed during ${currentLang.name} generation/parsing at step ${i + 1}"
                    )

                    // Generate in next language and parse
                    val nextCodeWithMetadata = nextLang.generateWithMetadataFn(parsedAst)
                    currentAst = nextLang.parseWithMetadataFn(nextCodeWithMetadata.code, nextCodeWithMetadata.metadata)
                }

                // Verify we got back to the original AST including metadata
                assertEquals(
                    originalAst, currentAst,
                    "AST not preserved through sequence: $sequenceNames -> ${sequence.first().name}"
                )

                println("✓ Sequential transpilation successful")

            } catch (e: Exception) {
                fail("Sequential transpilation failed for sequence $sequenceNames: ${e.message}")
            }
        }
        println("\\n=== '$testName' Sequential Transpilation Complete ===")
    }


    @Test
    fun `test simple print statement transpilation`() {
        // Define AST with minimal metadata for a simple print statement
        val printAst = ModuleNode(
            body = listOf(PrintNode(expression = ConstantNode("hello")))
        )

        testAstRoundTrip("Simple Print", printAst)
        testSequentialTranspilation("Simple Print", printAst)
    }

    @Test
    fun `test binary operation transpilation`() {
        // Define AST for print(1 + 2)
        val binaryOpAst = ModuleNode(
            body = listOf(
                PrintNode(
                    expression = BinaryOpNode(
                        left = ConstantNode(1),
                        op = "+",
                        right = ConstantNode(2)
                    )
                )
            )
        )

        testAstRoundTrip("Binary Operation", binaryOpAst)
        testSequentialTranspilation("Binary Operation", binaryOpAst)
    }

    @Test
    fun `test function without metadata transpilation`() {
        // Define AST for a function without metadata
        val functionAst = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "greet",
                    args = listOf(NameNode(id = "msg", ctx = Param)),
                    body = listOf(
                        PrintNode(
                            expression = BinaryOpNode(
                                left = ConstantNode("Hello "),
                                op = "+",
                                right = NameNode(id = "msg", ctx = Load)
                            )
                        )
                    ),
                    decoratorList = emptyList()
                ),
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "greet", ctx = Load),
                        args = listOf(ConstantNode("World")),
                        keywords = emptyList()
                    )
                )
            )
        )

        testAstRoundTrip("Function Without Metadata", functionAst)
        testSequentialTranspilation("Function Without Metadata", functionAst)
    }

    @Test
    fun `test function with return statement transpilation`() {
        // Use MaximalAstGenerator for function with return statement
        val functionWithReturnAst = MaximalAstGenerator.generateFunctionWithReturnStatement()

        testAstRoundTrip("Function With Return Statement", functionWithReturnAst)
        testSequentialTranspilation("Function With Return Statement", functionWithReturnAst)
    }

    @Test
    fun `test function with return value transpilation`() {
        // Use MaximalAstGenerator for function with return value
        val functionWithReturnValueAst = MaximalAstGenerator.generateFunctionWithReturnValue()

        testAstRoundTrip("Function With Return Value", functionWithReturnValueAst)
        testSequentialTranspilation("Function With Return Value", functionWithReturnValueAst)
    }

    @Test
    fun `test conditional statement transpilation`() {
        // Define AST for if-else without metadata
        val conditionalAst = ModuleNode(
            body = listOf(
                IfNode(
                    test = CompareNode(
                        left = NameNode(id = "x", ctx = Load),
                        op = ">",
                        right = ConstantNode(5)
                    ),
                    body = listOf(
                        PrintNode(expression = ConstantNode("greater"))
                    ),
                    orelse = listOf(
                        PrintNode(expression = ConstantNode("lesser"))
                    )
                )
            )
        )

        testAstRoundTrip("Conditional Statement", conditionalAst)
        testSequentialTranspilation("Conditional Statement", conditionalAst)
    }

    @Test
    fun `test variable assignment transpilation`() {
        // Define AST for variable assignment without metadata
        val assignmentAst = ModuleNode(
            body = listOf(
                AssignNode(
                    target = NameNode(id = "count", ctx = Store),
                    value = ConstantNode(42)
                ),
                PrintNode(
                    expression = NameNode(id = "count", ctx = Load)
                )
            )
        )

        testAstRoundTrip("Variable Assignment", assignmentAst)
        testSequentialTranspilation("Variable Assignment", assignmentAst)
    }

    @Test
    fun `test TypeScript to JavaScript metadata preservation`() {
        // Test isolated function call and constant features for metadata preservation
        val features = setOf(
            AstFeature.FUNCTION_CALLS,
            AstFeature.CONSTANT_VALUES
        )
        val functionWithMetadataAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("TypeScript to JavaScript Metadata", functionWithMetadataAst)
        testSequentialTranspilation("TypeScript to JavaScript Metadata", functionWithMetadataAst)
        println("✓ TypeScript metadata preservation through JavaScript successful")
    }

    @Test
    fun `test simple assignment metadata preservation`() {
        // Use the assignment utility that includes assignment metadata
        val assignmentAst = MaximalAstGenerator.generateAssignmentWithMetadata()

        testAstRoundTrip("Simple Assignment Metadata", assignmentAst)
        testSequentialTranspilation("Simple Assignment Metadata", assignmentAst)
    }

    @Test
    fun `test simple function metadata preservation`() {
        // Test isolated function definition with binary operations
        val features = setOf(
            AstFeature.FUNCTION_DEFINITIONS,
            AstFeature.BINARY_OPERATIONS,
            AstFeature.VARIABLE_REFERENCES,
            AstFeature.VARIABLE_ASSIGNMENTS
        )
        val functionAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Simple Function Metadata", functionAst)
        testSequentialTranspilation("Simple Function Metadata", functionAst)
    }

    @Test
    fun `test function and assignment metadata preservation`() {
        // Use the function utility that includes both function and assignment metadata
        val combinedAst = MaximalAstGenerator.generateFunctionWithMetadata()

        testAstRoundTrip("Function and Assignment Metadata", combinedAst)
        testSequentialTranspilation("Function and Assignment Metadata", combinedAst)
    }

    @Test
    fun `test maximal metadata preservation through all languages with parts`() {
        // Test comprehensive metadata preservation with all features including classes
        val maximalMetadataAst = MaximalAstGenerator.generateMaximalAst(SupportedAstFeatures.ALL_FEATURES)

        testAstRoundTrip("Maximal Metadata Preservation", maximalMetadataAst)
        testSequentialTranspilation("Maximal Metadata Preservation", maximalMetadataAst)
    }
    
    @Test
    fun `test isolated print statement feature transpilation`() {
        // Test only print statements to isolate this specific language feature
        val features = setOf(AstFeature.PRINT_STATEMENTS, AstFeature.CONSTANT_VALUES)
        val printOnlyAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Isolated Print Statement", printOnlyAst)
        testSequentialTranspilation("Isolated Print Statement", printOnlyAst)
    }
    
    @Test 
    fun `test isolated variable assignment feature transpilation`() {
        // Test only variable assignments to isolate this specific language feature
        val features = setOf(AstFeature.VARIABLE_ASSIGNMENTS, AstFeature.CONSTANT_VALUES)
        val assignmentOnlyAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Isolated Variable Assignment", assignmentOnlyAst)
        testSequentialTranspilation("Isolated Variable Assignment", assignmentOnlyAst)
    }
    
    @Test
    fun `test isolated binary operation feature transpilation`() {
        // Test function with binary operations to isolate this specific language feature
        val features = setOf(
            AstFeature.FUNCTION_DEFINITIONS,
            AstFeature.VARIABLE_ASSIGNMENTS,
            AstFeature.BINARY_OPERATIONS,
            AstFeature.VARIABLE_REFERENCES
        )
        val binaryOpAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Isolated Binary Operation", binaryOpAst)
        testSequentialTranspilation("Isolated Binary Operation", binaryOpAst)
    }
    
    @Test
    fun `test isolated conditional statement feature transpilation`() {
        // Test only conditional statements and comparisons to isolate this specific language feature
        val features = setOf(
            AstFeature.CONDITIONAL_STATEMENTS,
            AstFeature.COMPARISON_OPERATIONS,
            AstFeature.CONSTANT_VALUES
        )
        val conditionalAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Isolated Conditional Statement", conditionalAst)
        testSequentialTranspilation("Isolated Conditional Statement", conditionalAst)
    }

    @Test
    fun `test isolated class definition feature transpilation`() {
        // Test only class definitions to isolate this specific language feature
        val features = setOf(AstFeature.CLASS_DEFINITIONS)
        val classOnlyAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Isolated Class Definition", classOnlyAst)
        testSequentialTranspilation("Isolated Class Definition", classOnlyAst)
    }

    @Test
    fun `test class with methods transpilation`() {
        // Test class definitions with methods using maximal AST generator
        val features = setOf(
            AstFeature.CLASS_DEFINITIONS,
            AstFeature.FUNCTION_DEFINITIONS,
            AstFeature.VARIABLE_ASSIGNMENTS,
            AstFeature.RETURN_STATEMENTS,
            AstFeature.VARIABLE_REFERENCES
        )
        val classWithMethodsAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Class With Methods", classWithMethodsAst)
        testSequentialTranspilation("Class With Methods", classWithMethodsAst)
    }

    @Test
    fun `test isolated array literal feature transpilation`() {
        // Test only array literals to isolate this specific language feature
        val features = setOf(AstFeature.ARRAY_LITERALS, AstFeature.CONSTANT_VALUES)
        val arrayOnlyAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Isolated Array Literal", arrayOnlyAst)
        testSequentialTranspilation("Isolated Array Literal", arrayOnlyAst)
    }

    @Test
    fun `test isolated tuple literal feature transpilation`() {
        // Test only tuple literals to isolate this specific language feature
        val features = setOf(AstFeature.TUPLE_LITERALS, AstFeature.CONSTANT_VALUES)
        val tupleOnlyAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Isolated Tuple Literal", tupleOnlyAst)
        testSequentialTranspilation("Isolated Tuple Literal", tupleOnlyAst)
    }

    @Test
    fun `test isolated boolean literal feature transpilation`() {
        // Test only boolean literals to isolate this specific language feature
        val features = setOf(AstFeature.BOOLEAN_LITERALS)
        val booleanOnlyAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Isolated Boolean Literal", booleanOnlyAst)
        testSequentialTranspilation("Isolated Boolean Literal", booleanOnlyAst)
    }

    @Test
    fun `test combined new features transpilation`() {
        // Test arrays, tuples, and booleans together
        val features = setOf(
            AstFeature.ARRAY_LITERALS,
            AstFeature.TUPLE_LITERALS,
            AstFeature.BOOLEAN_LITERALS,
            AstFeature.CONSTANT_VALUES,
            AstFeature.VARIABLE_ASSIGNMENTS
        )
        val combinedNewFeaturesAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Combined New Features", combinedNewFeaturesAst)
        testSequentialTranspilation("Combined New Features", combinedNewFeaturesAst)
    }
}

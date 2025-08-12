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
import kotlin.test.fail

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
                    val parsedFromSource =
                        fromLang.parseWithMetadataFn(sourceCodeWithMetadata.code, sourceCodeWithMetadata.metadata)

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
                    val parsedFromTarget = toLang.parseWithMetadataFn(
                        intermediateCodeWithMetadata.code,
                        intermediateCodeWithMetadata.metadata
                    )

                    // Step 5: Generate final code with metadata back to source language
                    val finalCodeWithMetadata = fromLang.generateWithMetadataFn(parsedFromTarget)
                    println("Final ${fromLang.name} code: ${finalCodeWithMetadata.code}")
                    println("Final ${fromLang.name} metadata: ${finalCodeWithMetadata.metadata}")

                    // Step 6: Parse final code and verify AST preservation
                    val finalAst =
                        fromLang.parseWithMetadataFn(finalCodeWithMetadata.code, finalCodeWithMetadata.metadata)

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
    fun `test array and tuple literals transpilation`() {
        // Test array and tuple literals to verify cross-language support
        val features = setOf(
            AstFeature.ARRAY_LITERALS,
            AstFeature.TUPLE_LITERALS,
            AstFeature.VARIABLE_ASSIGNMENTS,
            AstFeature.CONSTANT_VALUES
        )
        val arrayTupleAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Array and Tuple Literals", arrayTupleAst)
        testSequentialTranspilation("Array and Tuple Literals", arrayTupleAst)
    }

    @Test
    fun `test isolated expression statement feature transpilation`() {
        // Test only expression statements to isolate this specific language feature
        val features = setOf(AstFeature.EXPRESSION_STATEMENTS, AstFeature.CONSTANT_VALUES, AstFeature.BINARY_OPERATIONS)
        val expressionStatementAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Isolated Expression Statement", expressionStatementAst)
        testSequentialTranspilation("Isolated Expression Statement", expressionStatementAst)
    }

    @Test
    fun `test expression statement with function calls transpilation`() {
        // Test expression statements combined with function calls for more comprehensive testing
        val features = setOf(
            AstFeature.EXPRESSION_STATEMENTS,
            AstFeature.FUNCTION_CALLS,
            AstFeature.CONSTANT_VALUES,
            AstFeature.VARIABLE_REFERENCES
        )
        val expressionWithCallsAst = MaximalAstGenerator.generateMaximalAst(features)

        testAstRoundTrip("Expression Statement with Function Calls", expressionWithCallsAst)
        testSequentialTranspilation("Expression Statement with Function Calls", expressionWithCallsAst)
    }

    @Test
    fun `test expression statement cross language compatibility`() {
        // Create a specific test for expression statements that tests cross-language compatibility
        // This tests simple expression statements which should be universally supported
        val expressionStatementAst = ModuleNode(
            body = listOf(
                ExprNode(
                    value = BinaryOpNode(
                        left = ConstantNode(10),
                        op = "+",
                        right = ConstantNode(5)
                    ),
                    metadata = mapOf("statementType" to "expression")
                )
            )
        )

        testAstRoundTrip("Expression Statement Cross Language", expressionStatementAst)
        testSequentialTranspilation("Expression Statement Cross Language", expressionStatementAst)
    }
}

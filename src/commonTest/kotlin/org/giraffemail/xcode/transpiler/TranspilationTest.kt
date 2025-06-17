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
    val parseFn: (String) -> AstNode,
    val generateFn: (AstNode) -> String
)

/**
 * Test suite for verifying AST preservation through transpilation chains.
 * Tests that ASTs with maximal metadata can round-trip through all supported languages
 * and preserve their metadata and structure.
 */
class TranspilationTest {

    private val pythonConfig = LanguageConfig("Python", PythonParser::parse) { ast -> PythonGenerator().generate(ast) }
    private val javaScriptConfig = LanguageConfig("JavaScript", JavaScriptParser::parse) { ast -> JavaScriptGenerator().generate(ast) }
    private val javaConfig = LanguageConfig("Java", JavaParser::parse) { ast -> JavaGenerator().generate(ast) }
    private val typeScriptConfig = LanguageConfig("TypeScript", TypeScriptParser::parse) { ast -> TypeScriptGenerator().generate(ast) }

    private val allLanguages = listOf(pythonConfig, javaScriptConfig, javaConfig, typeScriptConfig)

    /**
     * Test round-trip transpilation for a given AST through all language pairs.
     * Verifies that AST structure and metadata are preserved through all languages.
     */
    private fun testAstRoundTrip(testName: String, originalAst: AstNode) {
        println("\\n=== Testing AST Round-Trip for '$testName' ===")
        println("Original AST: $originalAst")

        for (fromLang in allLanguages) {
            for (toLang in allLanguages) {
                if (fromLang == toLang) continue

                try {
                    println("\\nTesting ${fromLang.name} -> ${toLang.name} -> ${fromLang.name}")

                    // Step 1: Generate code from original AST using source language
                    val sourceCode = fromLang.generateFn(originalAst)
                    println("Generated ${fromLang.name} code: $sourceCode")

                    // Step 2: Parse back to AST to verify generation didn't lose information
                    val parsedFromSource = fromLang.parseFn(sourceCode)
                    
                    // All languages should preserve metadata through comment serialization
                    assertEquals(
                        originalAst, parsedFromSource,
                        "AST changed after ${fromLang.name} generation/parsing round-trip"
                    )

                    // Step 3: Generate intermediate code using target language
                    val intermediateCode = toLang.generateFn(parsedFromSource)
                    println("Generated ${toLang.name} code: $intermediateCode")

                    // Step 4: Parse intermediate code back to AST
                    val parsedFromTarget = toLang.parseFn(intermediateCode)

                    // Step 5: Generate final code back to source language
                    val finalCode = fromLang.generateFn(parsedFromTarget)
                    println("Final ${fromLang.name} code: $finalCode")

                    // Step 6: Parse final code and verify AST preservation
                    val finalAst = fromLang.parseFn(finalCode)
                    
                    // Compare AST preservation through transpilation
                    // All languages should preserve metadata through comment serialization
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
                    
                    // Generate code in current language
                    val code = currentLang.generateFn(currentAst)
                    println("Generated ${currentLang.name} code: $code")

                    // Parse to verify AST preservation
                    val parsedAst = currentLang.parseFn(code)
                    
                    // All languages should preserve metadata through comment serialization
                    assertEquals(
                        currentAst, parsedAst,
                        "AST changed during ${currentLang.name} generation/parsing at step ${i + 1}"
                    )

                    // Generate in next language
                    val nextCode = nextLang.generateFn(parsedAst)
                    currentAst = nextLang.parseFn(nextCode)
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
        // Define AST with TypeScript metadata that should be preserved through JavaScript
        val functionWithMetadataAst = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "greet",
                    args = listOf(
                        NameNode(id = "name", ctx = Param, metadata = mapOf("type" to "string"))
                    ),
                    body = listOf(
                        PrintNode(
                            expression = BinaryOpNode(
                                left = ConstantNode("Hello "),
                                op = "+",
                                right = NameNode(id = "name", ctx = Load)
                            )
                        )
                    ),
                    decoratorList = emptyList(),
                    metadata = mapOf(
                        "returnType" to "void",
                        "paramTypes" to mapOf("name" to "string")
                    )
                )
            )
        )

        // Test specific TypeScript <-> JavaScript round trips for metadata preservation
        val tsConfig = typeScriptConfig
        val jsConfig = javaScriptConfig

        println("\\n=== Testing TypeScript ↔ JavaScript Metadata Preservation ===")

        // Test TypeScript -> JavaScript -> TypeScript
        println("\\nTesting TypeScript -> JavaScript -> TypeScript")
        val tsCode = tsConfig.generateFn(functionWithMetadataAst)
        println("Generated TypeScript: $tsCode")
        
        val tsParsed = tsConfig.parseFn(tsCode)
        assertEquals(functionWithMetadataAst, tsParsed, "TypeScript round-trip should preserve metadata")

        val jsCode = jsConfig.generateFn(tsParsed)
        println("Generated JavaScript with metadata: $jsCode")
        // Verify metadata is serialized
        assertTrue(jsCode.contains("__META__"), "JavaScript should contain metadata comments")

        val jsParsed = jsConfig.parseFn(jsCode)
        val finalTsCode = tsConfig.generateFn(jsParsed)
        println("Final TypeScript: $finalTsCode")
        
        val finalTsParsed = tsConfig.parseFn(finalTsCode)
        assertEquals(functionWithMetadataAst, finalTsParsed, "Metadata should be preserved through JS round-trip")

        println("✓ TypeScript metadata preservation through JavaScript successful")
    }

    @Test
    fun `test simple assignment metadata preservation`() {
        // Define AST with just assignment metadata to debug the issue
        val assignmentAst = ModuleNode(
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

        testAstRoundTrip("Simple Assignment Metadata", assignmentAst)
    }

    @Test
    fun `test simple function metadata preservation`() {
        // Define AST with just function metadata to debug the issue
        val functionAst = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "processData",
                    args = listOf(
                        NameNode(id = "input", ctx = Param, metadata = mapOf("type" to "string")),
                        NameNode(id = "count", ctx = Param, metadata = mapOf("type" to "number"))
                    ),
                    body = listOf(
                        PrintNode(
                            expression = NameNode(id = "input", ctx = Load)
                        )
                    ),
                    decoratorList = emptyList(),
                    metadata = mapOf(
                        "returnType" to "void",
                        "paramTypes" to mapOf("input" to "string", "count" to "number")
                    )
                )
            )
        )

        testAstRoundTrip("Simple Function Metadata", functionAst)
    }

    @Test
    fun `test function and assignment metadata preservation`() {
        // Define AST with both function and assignment metadata
        val combinedAst = ModuleNode(
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

        testAstRoundTrip("Function and Assignment Metadata", combinedAst)
    }

    @Test
    fun `test javascript metadata extraction debug`() {
        // Test the specific JavaScript code that's failing
        val jsCode = """function test() {
    let result = 'hello'; // __META__: {"variableType":"string"}
} // __META__: {"returnType":"void"}"""

        println("Testing JavaScript code: $jsCode")
        val parsedAst = JavaScriptParser.parse(jsCode)
        println("Parsed AST: $parsedAst")
        
        // Expected: function should have returnType metadata, assignment should have variableType metadata
        val expectedAst = ModuleNode(
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
        
        assertEquals(expectedAst, parsedAst)
    }

    @Test
    fun `test maximal metadata preservation through all languages`() {
        // Define AST with maximum TypeScript metadata that should be preserved
        val maximalMetadataAst = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "processData",
                    args = listOf(
                        NameNode(id = "input", ctx = Param, metadata = mapOf("type" to "string")),
                        NameNode(id = "count", ctx = Param, metadata = mapOf("type" to "number"))
                    ),
                    body = listOf(
                        AssignNode(
                            target = NameNode(id = "result", ctx = Store),
                            value = BinaryOpNode(
                                left = NameNode(id = "input", ctx = Load),
                                op = "+",
                                right = NameNode(id = "count", ctx = Load)
                            ),
                            metadata = mapOf("variableType" to "string")
                        ),
                        PrintNode(
                            expression = NameNode(id = "result", ctx = Load)
                        )
                    ),
                    decoratorList = emptyList(),
                    metadata = mapOf(
                        "returnType" to "void",
                        "paramTypes" to mapOf("input" to "string", "count" to "number")
                    )
                ),
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "processData", ctx = Load),
                        args = listOf(
                            ConstantNode("hello"),
                            ConstantNode(42)
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )

        testAstRoundTrip("Maximal Metadata Preservation", maximalMetadataAst)
        testSequentialTranspilation("Maximal Metadata Preservation", maximalMetadataAst)
    }
}

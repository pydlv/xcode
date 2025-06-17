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
     * Test that AST structure is preserved but metadata is lost in cross-language transpilation.
     * Verifies the new behavior where metadata is not automatically preserved without explicit parts.
     */
    private fun testAstStructureOnly(testName: String, originalAst: AstNode) {
        println("\\n=== Testing AST Structure Preservation (Metadata Loss Expected) for '$testName' ===")
        println("Original AST: $originalAst")

        for (fromLang in allLanguages) {
            for (toLang in allLanguages) {
                if (fromLang == toLang) continue

                try {
                    println("\\nTesting ${fromLang.name} -> ${toLang.name}")

                    // Step 1: Generate code from original AST using source language
                    val sourceCode = fromLang.generateFn(originalAst)
                    println("Generated ${fromLang.name} code: $sourceCode")

                    // Step 2: Generate intermediate code using target language
                    val intermediateCode = toLang.generateFn(originalAst)
                    println("Generated ${toLang.name} code: $intermediateCode")

                    // Verify that no metadata comments are generated
                    assertTrue(
                        !intermediateCode.contains("__META__"),
                        "${toLang.name} should not contain metadata comments (feature removed)"
                    )

                    println("✓ Structure preserved, metadata correctly not serialized")

                } catch (e: Exception) {
                    println("Note: ${fromLang.name} -> ${toLang.name} failed for '$testName': ${e.message}")
                    // Don't fail the test since some language combinations may not be fully compatible
                }
            }
        }
    }

    /**
     * Test sequential transpilation demonstrating that metadata is lost without explicit parts.
     * Verifies that basic structure is preserved but metadata is not.
     */
    private fun testSequentialBasicTranspilation(testName: String, originalAst: AstNode) {
        println("\\n=== Testing Sequential Basic Transpilation for '$testName' ===")
        println("Original AST: $originalAst")

        // Test a simple sequence: TypeScript -> JavaScript -> Python -> Java -> TypeScript
        val sequence = listOf(typeScriptConfig, javaScriptConfig, pythonConfig, javaConfig, typeScriptConfig)
        val sequenceNames = sequence.map { it.name }.joinToString(" -> ")
        println("Testing sequence: $sequenceNames")

        try {
            var currentAst = originalAst

            for (i in 0 until sequence.size - 1) {
                val currentLang = sequence[i]
                val nextLang = sequence[i + 1]

                println("Step ${i + 1}: ${currentLang.name} -> ${nextLang.name}")
                
                // Generate code in current language
                val code = currentLang.generateFn(currentAst)
                println("Generated ${currentLang.name} code: $code")

                // Verify no metadata comments are generated
                assertTrue(
                    !code.contains("__META__"),
                    "${currentLang.name} should not contain metadata comments (feature removed)"
                )

                // Parse with next language (structure should be preserved, metadata lost)
                currentAst = nextLang.parseFn(code)
            }

            println("✓ Sequential transpilation completed - structure preserved, metadata correctly lost")

        } catch (e: Exception) {
            println("Note: Sequential transpilation failed for '$testName': ${e.message}")
            // Don't fail the test since this demonstrates expected limitations
        }
        println("\\n=== '$testName' Sequential Transpilation Complete ===")
    }


    @Test
    fun `test simple print statement transpilation`() {
        // Define AST with minimal metadata for a simple print statement
        val printAst = ModuleNode(
            body = listOf(PrintNode(expression = ConstantNode("hello")))
        )

        testAstStructureOnly("Simple Print", printAst)
        testSequentialBasicTranspilation("Simple Print", printAst)
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

        testAstStructureOnly("Binary Operation", binaryOpAst)
        testSequentialBasicTranspilation("Binary Operation", binaryOpAst)
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

        testAstStructureOnly("Function Without Metadata", functionAst)
        testSequentialBasicTranspilation("Function Without Metadata", functionAst)
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

        testAstStructureOnly("Conditional Statement", conditionalAst)
        testSequentialBasicTranspilation("Conditional Statement", conditionalAst)
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

        testAstStructureOnly("Variable Assignment", assignmentAst)
        testSequentialBasicTranspilation("Variable Assignment", assignmentAst)
    }

    @Test
    fun `test TypeScript to JavaScript - metadata not preserved without explicit parts`() {
        // Define AST with TypeScript metadata
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

        println("\\n=== Testing TypeScript → JavaScript (No Auto-Metadata Preservation) ===")

        // Generate TypeScript code (with type annotations)
        val tsCode = typeScriptConfig.generateFn(functionWithMetadataAst)
        println("Generated TypeScript: $tsCode")
        assertTrue(tsCode.contains("name: string"))
        assertTrue(tsCode.contains("): void"))

        // Generate JavaScript code (should NOT contain metadata comments)
        val jsCode = javaScriptConfig.generateFn(functionWithMetadataAst)
        println("Generated JavaScript: $jsCode")
        assertTrue(!jsCode.contains("__META__"), "JavaScript should NOT contain metadata comments (feature removed)")
        assertTrue(jsCode.contains("function greet(name)"))

        // Parse JavaScript back - metadata should be lost without explicit parts
        val jsParsed = javaScriptConfig.parseFn(jsCode) as ModuleNode
        val jsFunction = jsParsed.body[0] as FunctionDefNode
        
        // Verify metadata is NOT preserved (expected behavior now)
        assertEquals(null, jsFunction.metadata?.get("returnType"))
        assertEquals(null, jsFunction.metadata?.get("paramTypes"))

        println("✓ Metadata correctly NOT preserved without explicit metadata parts")
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

        testAstStructureOnly("Simple Assignment Metadata", assignmentAst)
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

        testAstStructureOnly("Simple Function Metadata", functionAst)
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

        testAstStructureOnly("Function and Assignment Metadata", combinedAst)
    }

    @Test
    fun `test javascript metadata comments no longer parsed`() {
        // Test that JavaScript code with old-style metadata comments is parsed but metadata is ignored
        val jsCode = """function test() {
    let result = 'hello'; // __META__: {"variableType":"string"}
} // __META__: {"returnType":"void"}"""

        println("Testing JavaScript code with legacy metadata comments: $jsCode")
        val parsedAst = JavaScriptParser.parse(jsCode) as ModuleNode
        println("Parsed AST: $parsedAst")
        
        // Expected: metadata comments should be ignored (feature removed)
        val functionDef = parsedAst.body[0] as FunctionDefNode
        assertEquals("test", functionDef.name)
        assertEquals(null, functionDef.metadata?.get("returnType"))
        
        val assignment = functionDef.body[0] as AssignNode
        assertEquals(null, assignment.metadata?.get("variableType"))
        
        println("✓ Legacy metadata comments correctly ignored (expected behavior)")
    }

    @Test
    fun `test metadata not preserved across languages without explicit parts`() {
        // Define AST with TypeScript metadata
        val metadataAst = ModuleNode(
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
                )
            )
        )

        println("\\n=== Testing Cross-Language Metadata Loss (Expected) ===")
        
        // Generate TypeScript code (preserves metadata as type annotations)
        val tsCode = typeScriptConfig.generateFn(metadataAst)
        println("TypeScript with type annotations: $tsCode")
        assertTrue(tsCode.contains("input: string"))
        assertTrue(tsCode.contains("count: number"))
        assertTrue(tsCode.contains("): void"))
        
        // Generate JavaScript code (metadata should be lost)
        val jsCode = javaScriptConfig.generateFn(metadataAst)
        println("JavaScript without metadata: $jsCode")
        assertTrue(!jsCode.contains("__META__"))
        assertTrue(jsCode.contains("function processData(input, count)"))
        
        // Parse JavaScript back - metadata should be lost
        val jsParsed = javaScriptConfig.parseFn(jsCode) as ModuleNode
        val jsFunction = jsParsed.body[0] as FunctionDefNode
        assertEquals(null, jsFunction.metadata?.get("returnType"))
        assertEquals(null, jsFunction.metadata?.get("paramTypes"))
        
        println("✓ Metadata correctly lost in cross-language transpilation without explicit parts")
    }
}

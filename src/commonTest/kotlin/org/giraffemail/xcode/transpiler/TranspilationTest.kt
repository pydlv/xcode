package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.pythonparser.PythonParser
import org.giraffemail.xcode.pythonparser.PythonGenerator
import org.giraffemail.xcode.javascriptparser.JavaScriptParser
import org.giraffemail.xcode.javascriptparser.JavaScriptGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

data class LanguageConfig(
    val name: String,
    val parse: (String) -> AstNode,
    val generate: (AstNode) -> String
)

/**
 * Test suite for verifying code transpilation between different supported languages.
 * These tests ensure that code can be parsed into a common Abstract Syntax Tree (AST)
 * and then generated into another language, and potentially back to the original language,
 * maintaining semantic equivalence for the supported language features.
 */
class TranspilationTest {

    private val pythonConfig = LanguageConfig("Python", PythonParser::parse, PythonGenerator::generate)
    private val javaScriptConfig = LanguageConfig("JavaScript", JavaScriptParser::parse, JavaScriptGenerator::generate)

    private fun assertRoundTripTranspilation(
        originalCode: String,
        expectedIntermediateCode: String,
        lang1Config: LanguageConfig,
        lang2Config: LanguageConfig,
        expectedInitialAst: AstNode? = null,      // Expected AST from original code (lang1)
        expectedIntermediateAst: AstNode? = null  // Expected AST from intermediate code (lang2) after generation and re-parsing
    ) {
        println("Starting transpilation test: ${lang1Config.name} -> ${lang2Config.name} -> ${lang1Config.name}")
        println("Original ${lang1Config.name} code:\\n$originalCode")

        try {
            // 1. Lang1 to AST
            println("\\nStep 1: Parsing ${lang1Config.name} to AST...")
            val astFromLang1 = lang1Config.parse(originalCode)
            println("Generated AST from ${lang1Config.name}:\\n$astFromLang1")
            expectedInitialAst?.let {
                println("Expected initial AST from ${lang1Config.name}:\\n$it")
                assertEquals(it, astFromLang1, "Initial AST from ${lang1Config.name} parser is not as expected for code: \'$originalCode\'.")
            }
            println("Step 1 PASSED.")

            // 2. AST to Lang2
            println("\\nStep 2: Generating ${lang2Config.name} code from AST...")
            val generatedIntermediateCode = lang2Config.generate(astFromLang1)
            println("Generated ${lang2Config.name} code:\\n$generatedIntermediateCode")
            println("Expected ${lang2Config.name} code:\\n$expectedIntermediateCode")
            assertEquals(expectedIntermediateCode, generatedIntermediateCode, "${lang1Config.name} AST to ${lang2Config.name} code generation failed.")
            println("Step 2 PASSED.")

            // 3. Lang2 to AST
            println("\\nStep 3: Parsing ${lang2Config.name} to AST...")
            val astFromLang2 = lang2Config.parse(generatedIntermediateCode)
            println("Generated AST from ${lang2Config.name}:\\n$astFromLang2")
            expectedIntermediateAst?.let {
                println("Expected intermediate AST from ${lang2Config.name}:\\n$it")
                assertEquals(it, astFromLang2, "AST from ${lang2Config.name} parser is not as expected for code: \'$generatedIntermediateCode\'.")
            }
            println("Step 3 PASSED.")

            // 4. AST to Lang1 (back to original)
            println("\\nStep 4: Generating ${lang1Config.name} code from ${lang2Config.name} AST (round trip)...")
            val finalOriginalCode = lang1Config.generate(astFromLang2)
            println("Generated final ${lang1Config.name} code:\\n$finalOriginalCode")
            println("Expected final ${lang1Config.name} code (original):\\n$originalCode")
            assertEquals(originalCode, finalOriginalCode, "${lang2Config.name} AST to ${lang1Config.name} code generation failed (round trip).")
            println("Step 4 PASSED.")

            println("\\nTranspilation test successfully completed for: \'$originalCode\'")

        } catch (e: AstParseException) {
            fail("Transpilation test (${lang1Config.name} to ${lang2Config.name} to ${lang1Config.name} for code '$originalCode') failed due to parsing error: ${e.message}", e)
        } catch (e: Exception) {
            fail("Transpilation test (${lang1Config.name} to ${lang2Config.name} to ${lang1Config.name} for code '$originalCode') failed due to an unexpected error: ${e.message}", e)
        }
    }

    @Test
    fun `test python to javascript and back to python for print statement`() {
        val originalPythonCode = "print('cookies')"
        val expectedIntermediateJsCode = "console.log('cookies');"
        // Adjusted expectedPyAst
        val expectedPyAst = ModuleNode(body=listOf(PrintNode(expression=ConstantNode("cookies"))))
        // Adjusted expectedJsAstAfterRoundtrip
        val expectedJsAstAfterRoundtrip = ModuleNode(body=listOf(PrintNode(expression=ConstantNode("cookies"))))

        assertRoundTripTranspilation(
            originalCode = originalPythonCode,
            expectedIntermediateCode = expectedIntermediateJsCode,
            lang1Config = pythonConfig,
            lang2Config = javaScriptConfig,
            expectedInitialAst = expectedPyAst,
            expectedIntermediateAst = expectedJsAstAfterRoundtrip
        )
    }

    @Test
    fun `test javascript to python and back to javascript for console log statement`() {
        val originalJsCode = "console.log('more_cookies');"
        val expectedIntermediatePythonCode = "print('more_cookies')"
        // Adjusted expectedJsAst (expectedInitialAst)
        val expectedJsAst = ModuleNode(body=listOf(PrintNode(expression=ConstantNode("more_cookies"))))
        // Adjusted expectedPyAstAfterRoundtrip
        val expectedPyAstAfterRoundtrip = ModuleNode(body=listOf(PrintNode(expression=ConstantNode("more_cookies"))))

        assertRoundTripTranspilation(
            originalCode = originalJsCode,
            expectedIntermediateCode = expectedIntermediatePythonCode,
            lang1Config = javaScriptConfig,
            lang2Config = pythonConfig,
            expectedInitialAst = expectedJsAst,
            expectedIntermediateAst = expectedPyAstAfterRoundtrip
        )
    }

    @Test
    fun `test python to javascript and back to python for print with addition`() {
        val originalPythonCode = "print(1 + 2)"
        val expectedIntermediateJsCode = "console.log(1 + 2);"
        // Adjusted expectedPyAst
        val expectedPyAst = ModuleNode(body=listOf(PrintNode(expression=BinaryOpNode(ConstantNode(1), "+", ConstantNode(2)))))
        // Adjusted expectedJsAstAfterRoundtrip - JS parser produces Doubles
        val expectedJsAstAfterRoundtrip = ModuleNode(body=listOf(PrintNode(expression=BinaryOpNode(ConstantNode(1.0), "+", ConstantNode(2.0)))))

        assertRoundTripTranspilation(
            originalCode = originalPythonCode,
            expectedIntermediateCode = expectedIntermediateJsCode,
            lang1Config = pythonConfig,
            lang2Config = javaScriptConfig,
            expectedInitialAst = expectedPyAst,
            expectedIntermediateAst = expectedJsAstAfterRoundtrip
        )
    }

    @Test
    fun `test javascript to python and back to javascript for console log with addition`() {
        val originalJsCode = "console.log(1 + 2);"
        val expectedIntermediatePythonCode = "print(1 + 2)"
        // Adjusted expectedInitialJsAst - JS parser produces Doubles
        val expectedInitialJsAst = ModuleNode(body=listOf(PrintNode(expression=BinaryOpNode(ConstantNode(1.0), "+", ConstantNode(2.0)))))
        // Adjusted expectedPyAstAfterRoundtrip
        val expectedPyAstAfterRoundtrip = ModuleNode(body=listOf(PrintNode(expression=BinaryOpNode(ConstantNode(1), "+", ConstantNode(2)))))

        assertRoundTripTranspilation(
            originalCode = originalJsCode,
            expectedIntermediateCode = expectedIntermediatePythonCode,
            lang1Config = javaScriptConfig,
            lang2Config = pythonConfig,
            expectedInitialAst = expectedInitialJsAst,
            expectedIntermediateAst = expectedPyAstAfterRoundtrip
        )
    }

    @Test
    fun `test bidirectional function with print statement transpilation`() {
        // Python code and expected JavaScript transpilation
        val pythonCode = """
            def greet(name):
                print('Hello, ' + name)
        """.trimIndent()

        val javascriptCode = """
            function greet(name) {
                console.log('Hello, ' + name);
            }
        """.trimIndent()

        // Define expected function body for both languages
        val functionBody = listOf<StatementNode>(
            PrintNode(
                expression = BinaryOpNode(
                    left = ConstantNode("Hello, "),
                    op = "+",
                    right = NameNode(id = "name", ctx = Load)
                )
            )
        )

        // Define expected AST structure (same for both Python and JS in this case)
        val expectedAst = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "greet",
                    args = listOf(NameNode(id = "name", ctx = Load)),
                    body = functionBody,
                    decorator_list = emptyList()
                )
            )
        )

        // Test Python to JavaScript transpilation
        assertRoundTripTranspilation(
            originalCode = pythonCode,
            expectedIntermediateCode = javascriptCode,
            lang1Config = pythonConfig,
            lang2Config = javaScriptConfig,
            expectedInitialAst = expectedAst,
            expectedIntermediateAst = expectedAst
        )

        // Test JavaScript to Python transpilation
        assertRoundTripTranspilation(
            originalCode = javascriptCode,
            expectedIntermediateCode = pythonCode,
            lang1Config = javaScriptConfig,
            lang2Config = pythonConfig,
            expectedInitialAst = expectedAst,
            expectedIntermediateAst = expectedAst
        )
    }

    @Test
    fun `test recursive fibonacci function transpilation`() {
        // Python code for recursive fibonacci
        val pythonCode = """
            def fib(a,b):
                c = a + b
                print(c)
                fib(b, c)
            
            fib(0, 1)
        """.trimIndent().trim() // MODIFIED: Added .trim() to remove leading/trailing whitespace

        // Expected JavaScript transpilation
        val javascriptCode = """
            function fib(a, b) {
                let c = a + b;
                console.log(c);
                fib(b, c);
            }
            
            fib(0, 1);
        """.trimIndent().trim() // MODIFIED: Added .trim() to remove leading/trailing whitespace

        // Define expected function body for both languages
        val functionBody = listOf<StatementNode>(
            AssignNode(
                target = NameNode(id = "c", ctx = Store),
                value = BinaryOpNode(
                    left = NameNode(id = "a", ctx = Load),
                    op = "+",
                    right = NameNode(id = "b", ctx = Load)
                )
            ),
            PrintNode(
                expression = NameNode(id = "c", ctx = Load)
            ),
            CallStatementNode(
                call = CallNode(
                    func = NameNode(id = "fib", ctx = Load),
                    args = listOf(
                        NameNode(id = "b", ctx = Load),
                        NameNode(id = "c", ctx = Load)
                    ),
                    keywords = emptyList()
                )
            )
        )

        // Define expected AST structure for Python (integers for constants)
        val expectedPyAst = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "fib",
                    args = listOf(NameNode(id = "a", ctx = Load), NameNode(id = "b", ctx = Load)),
                    body = functionBody,
                    decorator_list = emptyList()
                ),
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "fib", ctx = Load),
                        args = listOf(
                            ConstantNode(0), // Python uses Integer
                            ConstantNode(1)  // Python uses Integer
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )

        // Define expected AST structure for JavaScript (floats for constants)
        val expectedJsAst = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "fib",
                    args = listOf(NameNode(id = "a", ctx = Load), NameNode(id = "b", ctx = Load)),
                    body = functionBody, // functionBody is the same
                    decorator_list = emptyList()
                ),
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "fib", ctx = Load),
                        args = listOf(
                            ConstantNode(0.0), // JavaScript uses Float
                            ConstantNode(1.0)  // JavaScript uses Float
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )

        // Test Python to JavaScript transpilation
        assertRoundTripTranspilation(
            originalCode = pythonCode,
            expectedIntermediateCode = javascriptCode,
            lang1Config = pythonConfig,
            lang2Config = javaScriptConfig,
            expectedInitialAst = expectedPyAst,    // Parsed from Python code
            expectedIntermediateAst = expectedJsAst // Parsed from generated JS code
        )

        // Test JavaScript to Python transpilation
        assertRoundTripTranspilation(
            originalCode = javascriptCode,
            expectedIntermediateCode = pythonCode,
            lang1Config = javaScriptConfig,
            lang2Config = pythonConfig,
            expectedInitialAst = expectedJsAst,    // Parsed from JS code
            expectedIntermediateAst = expectedPyAst     // Parsed from generated Python code
        )
    }
}

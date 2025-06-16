package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.ast.AssignNode
import org.giraffemail.xcode.ast.AstNode
import org.giraffemail.xcode.ast.BinaryOpNode
import org.giraffemail.xcode.ast.CallNode
import org.giraffemail.xcode.ast.CallStatementNode
import org.giraffemail.xcode.ast.ConstantNode
import org.giraffemail.xcode.ast.FunctionDefNode
import org.giraffemail.xcode.ast.Load
import org.giraffemail.xcode.ast.ModuleNode
import org.giraffemail.xcode.ast.NameNode
import org.giraffemail.xcode.ast.PrintNode
import org.giraffemail.xcode.ast.StatementNode
import org.giraffemail.xcode.ast.Store
import org.giraffemail.xcode.javaparser.JavaGenerator
import org.giraffemail.xcode.javaparser.JavaParser
import org.giraffemail.xcode.javascriptparser.JavaScriptGenerator
import org.giraffemail.xcode.javascriptparser.JavaScriptParser
import org.giraffemail.xcode.pythonparser.PythonGenerator
import org.giraffemail.xcode.pythonparser.PythonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

data class LanguageConfig(
    val name: String,
    val parseFn: (String) -> AstNode,
    val generateFn: (AstNode) -> String
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
    private val javaConfig = LanguageConfig("Java", JavaParser::parse, JavaGenerator::generate)

    private val expectedPrintCookiesAst = ModuleNode(
        body = listOf(PrintNode(expression = ConstantNode("cookies")))
    )

    private fun assertRoundTripTranspilation(
        originalCode: String,
        expectedIntermediateCode: String,
        lang1Config: LanguageConfig,
        lang2Config: LanguageConfig,
        expectedInitialAst: AstNode? = null,      // Expected AST from original code (lang1)
        expectedIntermediateAst: AstNode? = null  // Expected AST from intermediate code (lang2) after generation and re-parsing
    ) {
        println("Starting round-trip transpilation test: ${lang1Config.name} -> ${lang2Config.name} -> ${lang1Config.name}")
        println("Original ${lang1Config.name} code:\\n$originalCode")

        try {
            // 1. Lang1 to AST
            println("\\nStep 1: Parsing ${lang1Config.name} to AST...")
            val astFromLang1 = lang1Config.parseFn(originalCode)
            println("Generated AST from ${lang1Config.name}:\\n$astFromLang1")
            expectedInitialAst?.let {
                println("Expected initial AST from ${lang1Config.name}:\\n$it")
                assertEquals(it, astFromLang1, "Initial AST from ${lang1Config.name} parser is not as expected for code: \'$originalCode\'.")
            }
            println("Step 1 PASSED.")

            // 2. AST to Lang2
            println("\\nStep 2: Generating ${lang2Config.name} code from AST...")
            val generatedIntermediateCode = lang2Config.generateFn(astFromLang1)
            println("Generated ${lang2Config.name} code:\\n$generatedIntermediateCode")
            println("Expected ${lang2Config.name} code:\\n$expectedIntermediateCode")
            assertEquals(expectedIntermediateCode, generatedIntermediateCode, "${lang1Config.name} AST to ${lang2Config.name} code generation failed.")
            println("Step 2 PASSED.")

            // 3. Lang2 to AST
            println("\\nStep 3: Parsing ${lang2Config.name} to AST...")
            val astFromLang2 = lang2Config.parseFn(generatedIntermediateCode)
            println("Generated AST from ${lang2Config.name}:\\n$astFromLang2")
            expectedIntermediateAst?.let {
                println("Expected intermediate AST from ${lang2Config.name}:\\n$it")
                assertEquals(it, astFromLang2, "AST from ${lang2Config.name} parser is not as expected for code: \'$generatedIntermediateCode\'.")
            }
            println("Step 3 PASSED.")

            // 4. AST to Lang1 (back to original)
            println("\\nStep 4: Generating ${lang1Config.name} code from ${lang2Config.name} AST (round trip)...")
            val finalOriginalCode = lang1Config.generateFn(astFromLang2)
            println("Generated final ${lang1Config.name} code:\\n$finalOriginalCode")
            println("Expected final ${lang1Config.name} code (original):\\n$originalCode")
            assertEquals(originalCode, finalOriginalCode, "${lang2Config.name} AST to ${lang1Config.name} code generation failed (round trip).")
            println("Step 4 PASSED.")

            println("\\nTranspilation test successfully completed for: \'$originalCode\'")

        } catch (e: Exception) {
            fail("Transpilation test ${lang1Config.name} -> ${lang2Config.name} -> ${lang1Config.name} failed: ${e.message}\\n${e.stackTraceToString()}")
        }
        println("Transpilation test ${lang1Config.name} -> ${lang2Config.name} -> ${lang1Config.name} completed successfully.\\n")
    }

    private fun assertSequentialTranspilation(
        initialCode: String,
        languageSequence: List<LanguageConfig>,
        expectedIntermediateGeneratedCodes: List<String>,
        expectedAstsForEachStage: List<AstNode>
    ) {
        if (languageSequence.size < 2) {
            fail("Language sequence must have at least 2 languages for sequential transpilation.")
        }
        if (expectedIntermediateGeneratedCodes.size != languageSequence.size - 1) {
            fail("Mismatch in expected intermediate codes count. Expected ${languageSequence.size - 1}, got ${expectedIntermediateGeneratedCodes.size}")
        }
        if (expectedAstsForEachStage.size != languageSequence.size) {
            fail("Mismatch in expected ASTs count. Expected ${languageSequence.size}, got ${expectedAstsForEachStage.size}")
        }

        val sequenceDescription = languageSequence.joinToString(" -> ") { it.name }
        println("Starting sequential transpilation test: $sequenceDescription -> ${languageSequence.first().name}")
        println("Initial ${languageSequence.first().name} code:\\n$initialCode")

        var currentCode = initialCode
        var currentAst: AstNode? = null

        try {
            for (i in languageSequence.indices) {
                val currentLangConfig = languageSequence[i]
                val nextLangConfig = languageSequence.getOrNull((i + 1) % languageSequence.size)!! // Wraps around for the last step to initial

                println("\\nStep ${i + 1}: Parsing ${currentLangConfig.name} to AST...")
                currentAst = currentLangConfig.parseFn(currentCode)
                println("Parsed AST: $currentAst")
                assertEquals(expectedAstsForEachStage[i], currentAst, "AST for ${currentLangConfig.name} (stage ${i+1}) did not match expected.")

                if (i < languageSequence.size -1) { // Intermediate generation L1->L2, L2->L3 etc.
                    val targetLangForGeneration = languageSequence[i+1]
                    println("\\nStep ${i + 1}.${i + 2}: Generating ${targetLangForGeneration.name} from ${currentLangConfig.name} AST...")
                    val generatedIntermediateCode = targetLangForGeneration.generateFn(currentAst)
                    println("Generated ${targetLangForGeneration.name} code:\\n$generatedIntermediateCode")
                    assertEquals(expectedIntermediateGeneratedCodes[i], generatedIntermediateCode,
                        "Generated ${targetLangForGeneration.name} code from ${currentLangConfig.name} AST did not match expected.")
                    currentCode = generatedIntermediateCode // This becomes input for next parsing stage
                } else { // Final generation: Last language in sequence back to First language
                    val finalTargetLang = languageSequence.first()
                    println("\\nStep ${i + 1}.${i + 2}: Generating final ${finalTargetLang.name} code from ${currentLangConfig.name} AST...")
                    val finalGeneratedCode = finalTargetLang.generateFn(currentAst)
                    println("Final ${finalTargetLang.name} code (after sequence):\\n$finalGeneratedCode")
                    assertEquals(initialCode, finalGeneratedCode,
                        "Final ${finalTargetLang.name} code should match initial ${languageSequence.first().name} code after sequential transpilation.")

                    // Optionally, re-parse the final code and check its AST
                    val finalParsedAst = finalTargetLang.parseFn(finalGeneratedCode)
                    assertEquals(expectedAstsForEachStage.first(), finalParsedAst, "AST of final ${finalTargetLang.name} code should match expected initial AST of ${languageSequence.first().name}.")
                }
            }
        } catch (e: Exception) {
            fail("Sequential transpilation test $sequenceDescription -> ${languageSequence.first().name} failed: ${e.message}\\n${e.stackTraceToString()}")
        }
        println("Sequential transpilation test $sequenceDescription -> ${languageSequence.first().name} completed successfully.\\n")
    }


    @Test
    fun `test bidirectional print statement transpilation`() {
        val pythonPrintCode = "print('cookies')"
        val jsPrintCode = "console.log('cookies');"
        val javaPrintCode = "System.out.println(\"cookies\");"

        val allLanguageSetupsForPrintTest = listOf(
            Triple(pythonConfig, pythonPrintCode, expectedPrintCookiesAst),
            Triple(javaScriptConfig, jsPrintCode, expectedPrintCookiesAst),
            Triple(javaConfig, javaPrintCode, expectedPrintCookiesAst)
            // To add a new language for the print test, add its Triple here
        )

        val nLanguages = allLanguageSetupsForPrintTest.size
        if (nLanguages >= 2) {
            println("\\n--- Starting Pairwise Round-Trip Tests for 'Print Cookies' (n=${nLanguages}) ---")
            for (i in allLanguageSetupsForPrintTest.indices) {
                for (j in allLanguageSetupsForPrintTest.indices) {
                    if (i == j) continue

                    val (lang1Config, code1, ast1) = allLanguageSetupsForPrintTest[i]
                    val (lang2Config, code2, ast2) = allLanguageSetupsForPrintTest[j]

                    assertRoundTripTranspilation(
                        originalCode = code1,
                        expectedIntermediateCode = code2,
                        lang1Config = lang1Config,
                        lang2Config = lang2Config,
                        expectedInitialAst = ast1,
                        expectedIntermediateAst = ast2
                    )
                }
            }
            println("\\n--- Pairwise Round-Trip Tests for 'Print Cookies' Completed ---")

            println("\\n--- Starting Sequential Transpilation Tests for 'Print Cookies' (n=${nLanguages}) ---")
            for (startIndex in 0 until nLanguages) {
                val currentLanguageSequence = mutableListOf<LanguageConfig>()
                val currentExpectedIntermediateCodes = mutableListOf<String>()
                val currentExpectedAsts = mutableListOf<AstNode>()

                for (offset in 0 until nLanguages) {
                    val currentIndex = (startIndex + offset) % nLanguages
                    val (langConfig, code, ast) = allLanguageSetupsForPrintTest[currentIndex]
                    currentLanguageSequence.add(langConfig)
                    currentExpectedAsts.add(ast)
                    if (offset < nLanguages - 1) {
                        val nextIndexInSequence = (startIndex + offset + 1) % nLanguages
                        currentExpectedIntermediateCodes.add(allLanguageSetupsForPrintTest[nextIndexInSequence].second)
                    }
                }
                val initialCodeForSequence = allLanguageSetupsForPrintTest[startIndex].second
                assertSequentialTranspilation(
                    initialCode = initialCodeForSequence,
                    languageSequence = currentLanguageSequence,
                    expectedIntermediateGeneratedCodes = currentExpectedIntermediateCodes,
                    expectedAstsForEachStage = currentExpectedAsts
                )
            }
            println("\\n--- Sequential Transpilation Tests for 'Print Cookies' (n=${nLanguages}) Completed ---")
        } else if (nLanguages == 1) {
            val (config, code, ast) = allLanguageSetupsForPrintTest.first()
            println("Testing single language parse/generate for 'Print Cookies': ${config.name}")
            try {
                val parsed = config.parseFn(code)
                assertEquals(ast, parsed, "AST mismatch for single language parse (print cookies).")
                val generated = config.generateFn(parsed)
                assertEquals(code, generated, "Code mismatch for single language generate (print cookies).")
            } catch (e: Exception) {
                fail("Single language test for ${config.name} (print cookies) failed: ${e.message}")
            }
        }
        println("\\n'test bidirectional print statement transpilation' completed.")
    }

    @Test
    fun `test bidirectional print with addition`() {
        println("\\n--- Running Python <-> JavaScript 'Addition Expression' Test ---")
        val pythonCode = "print(1 + 2)"
        val jsCode = "console.log(1 + 2);"
        // ASTs for addition are not checked in this simplified version for now.
        assertRoundTripTranspilation(
            originalCode = pythonCode,
            expectedIntermediateCode = jsCode,
            lang1Config = pythonConfig,
            lang2Config = javaScriptConfig
            // expectedInitialAst = expectedPyAdditionAst, // Define these if needed
            // expectedIntermediateAst = expectedJsAdditionAst
        )
        println("\\n'test bidirectional print with addition' completed.")
    }

    @Test
    fun `test recursive fibonacci function transpilation`() {
        // Python code for recursive fibonacci
        val pythonCode = """
            def fib(a, b):
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

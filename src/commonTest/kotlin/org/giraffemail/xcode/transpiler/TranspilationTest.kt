package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.javaparser.JavaGenerator
import org.giraffemail.xcode.javaparser.JavaParser
import org.giraffemail.xcode.javascriptparser.JavaScriptGenerator
import org.giraffemail.xcode.javascriptparser.JavaScriptParser
import org.giraffemail.xcode.pythonparser.PythonGenerator
import org.giraffemail.xcode.pythonparser.PythonParser
import kotlin.test.Test
import kotlin.test.assertEquals
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

    private val pythonConfig = LanguageConfig("Python", PythonParser::parse, { ast -> PythonGenerator().generate(ast) }) // Changed
    private val javaScriptConfig = LanguageConfig("JavaScript", JavaScriptParser::parse, { ast -> JavaScriptGenerator().generate(ast) }) // Changed
    private val javaConfig = LanguageConfig("Java", JavaParser::parse, { ast -> JavaGenerator().generate(ast) }) // Changed

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
        var currentAst: AstNode?

        try {
            for (i in languageSequence.indices) {
                val currentLangConfig = languageSequence[i]

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


    private fun executeTranspilationTests(
        testName: String,
        allLanguageSetups: List<Triple<LanguageConfig, String, AstNode>>
    ) {
        val nLanguages = allLanguageSetups.size
        if (nLanguages >= 2) {
            println("\\\\n--- Starting Pairwise Round-Trip Tests for \'$testName\' (n=${nLanguages}) ---")
            for (i in allLanguageSetups.indices) {
                for (j in allLanguageSetups.indices) {
                    if (i == j) continue

                    val (lang1Config, code1, ast1) = allLanguageSetups[i]
                    val (lang2Config, code2, ast2) = allLanguageSetups[j]

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
            println("\\\\n--- Pairwise Round-Trip Tests for \'$testName\' Completed ---")

            println("\\\\n--- Starting Sequential Transpilation Tests for \'$testName\' (n=${nLanguages}) ---")
            for (startIndex in 0 until nLanguages) {
                val currentLanguageSequence = mutableListOf<LanguageConfig>()
                val currentExpectedIntermediateCodes = mutableListOf<String>()
                val currentExpectedAsts = mutableListOf<AstNode>()

                for (offset in 0 until nLanguages) {
                    val currentIndex = (startIndex + offset) % nLanguages
                    val (langConfig, _, ast) = allLanguageSetups[currentIndex]
                    currentLanguageSequence.add(langConfig)
                    currentExpectedAsts.add(ast)
                    if (offset < nLanguages - 1) {
                        val nextIndexInSequence = (startIndex + offset + 1) % nLanguages
                        currentExpectedIntermediateCodes.add(allLanguageSetups[nextIndexInSequence].second)
                    }
                }
                val initialCodeForSequence = allLanguageSetups[startIndex].second
                assertSequentialTranspilation(
                    initialCode = initialCodeForSequence,
                    languageSequence = currentLanguageSequence,
                    expectedIntermediateGeneratedCodes = currentExpectedIntermediateCodes,
                    expectedAstsForEachStage = currentExpectedAsts
                )
            }
            println("\\\\n--- Sequential Transpilation Tests for \'$testName\' (n=${nLanguages}) Completed ---")
        } else if (nLanguages == 1) {
            val (config, code, ast) = allLanguageSetups.first()
            println("Testing single language parse/generate for \'$testName\': ${config.name}")
            try {
                val parsed = config.parseFn(code)
                assertEquals(ast, parsed, "AST mismatch for single language parse ($testName).")
                val generated = config.generateFn(parsed)
                assertEquals(code, generated, "Code mismatch for single language generate ($testName).")
            } catch (e: Exception) {
                fail("Single language test for ${config.name} ($testName) failed: ${e.message}")
            }
        }
        println("\\\\n\'Test $testName\' completed.")
    }


    @Test
    fun `test bidirectional print statement transpilation`() {
        val pythonPrintCode = "print('cookies')"
        val jsPrintCode = "console.log('cookies');"
        val javaPrintCode = "System.out.println(\"cookies\");"

        // Corrected expected AST for Java to match the others for this simple print case.
        // The Java parser should produce a ConstantNode with a string value, identical to Python/JS.
        val commonExpectedPrintAst = ModuleNode(
            body = listOf(PrintNode(expression = ConstantNode("cookies")))
        )

        val allLanguageSetupsForPrintTest = listOf(
            Triple(pythonConfig, pythonPrintCode, commonExpectedPrintAst),
            Triple(javaScriptConfig, jsPrintCode, commonExpectedPrintAst),
            Triple(javaConfig, javaPrintCode, commonExpectedPrintAst) // Using the common AST
            // To add a new language for the print test, add its Triple here
        )

        executeTranspilationTests("Print Cookies", allLanguageSetupsForPrintTest)
    }

    @Test
    fun `test bidirectional print with addition`() {
        // Define ASTs for "print(1 + 2)" for each language
        val commonAstAdd = ModuleNode(
            body = listOf(
                PrintNode(
                    expression = BinaryOpNode(
                        left = ConstantNode(1), // Standardized to Integer for cross-language AST
                        op = "+",
                        right = ConstantNode(2)  // Standardized to Integer for cross-language AST
                    )
                )
            )
        )

        // JavaScript-specific AST if its parser produces floats for numbers, but generator should handle conversion if needed.
        // For simplicity in this test, we might assume the generators can handle a common AST form (e.g. integer constants)
        // or we adjust the common AST to use a more generic number type if the AST definition supports it.
        // Here, we'll assume the common AST uses integers and JS generator/parser handles it.
        // If JS strictly uses floats, the jsAstAdd would be:
        val jsSpecificAstAdd = ModuleNode(
            body = listOf(
                PrintNode(
                    expression = BinaryOpNode(
                        left = ConstantNode(1.0),
                        op = "+",
                        right = ConstantNode(2.0)
                    )
                )
            )
        )

        // Define code snippets
        val pythonCodeAdd = "print(1 + 2)"
        val jsCodeAdd = "console.log(1 + 2);"
        val javaCodeAdd = "System.out.println(1 + 2);"

        // Create language setups list
        val allLanguageSetupsForPrintAddTest = listOf(
            Triple(pythonConfig, pythonCodeAdd, commonAstAdd),
            // For JavaScript, if its parser *always* creates float ConstantNodes for numbers,
            // then its specific AST (jsSpecificAstAdd) should be used here.
            // However, if the goal is a common intermediate AST, then commonAstAdd is preferred,
            // and the JS parser/generator must align.
            // Assuming JS parser produces floats, and we want to test that specific behavior:
            Triple(javaScriptConfig, jsCodeAdd, jsSpecificAstAdd),
            Triple(javaConfig, javaCodeAdd, commonAstAdd) // Java uses the common AST with Integers
            // To add a new language for the print with addition test, add its Triple here
        )

        executeTranspilationTests("Print Addition", allLanguageSetupsForPrintAddTest)
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
        """.trimIndent().trim()

        // Expected JavaScript transpilation
        val javascriptCode = """
            function fib(a, b) {
                let c = a + b;
                console.log(c);
                fib(b, c);
            }
            fib(0, 1);
        """.trimIndent().trim()

        // Define expected function body for both languages
        val functionBody = listOf(
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
                    body = functionBody // functionBody is the same
                    , decorator_list = emptyList()
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

        // Java code for recursive fibonacci
        val javaCode = """public static void fib(Object a, Object b) {
        c = a + b;
        System.out.println(c);
        fib(b, c);
    }
fib(0, 1);"""

        // Define expected AST structure for Java (integers for constants)
        // This assumes the Java parser/generator can map the class structure to/from this common AST form
        // for the purpose of this test, to align with Python/JS ASTs.
        val expectedJavaAst = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "fib",
                    args = listOf(NameNode(id = "a", ctx = Param), NameNode(id = "b", ctx = Param)),
                    body = functionBody, // functionBody is the same
                    decorator_list = emptyList()
                ),
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "fib", ctx = Load),
                        args = listOf(
                            ConstantNode(0), // Java uses Integer
                            ConstantNode(1)  // Java uses Integer
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )

        val allLanguageSetupsForFibonacciTest = listOf(
            Triple(pythonConfig, pythonCode, expectedPyAst),
            Triple(javaScriptConfig, javascriptCode, expectedJsAst),
            Triple(javaConfig, javaCode, expectedJavaAst)
            // To add a new language for the fibonacci test, add its Triple here
        )

        executeTranspilationTests("Recursive Fibonacci", allLanguageSetupsForFibonacciTest)
    }

    @Test
    fun `test if else statement transpilation`() {
        // Simple if-else statements with variable comparison
        val pythonCode = """
            if x > 5:
                print('greater')
            else:
                print('lesser')
        """.trimIndent().trim()

        val javascriptCode = """
            if (x > 5) {
                console.log('greater');
            } else {
                console.log('lesser');
            }
        """.trimIndent().trim()

        val javaCode = """
            if (x > 5) {
                System.out.println("greater");
            } else {
                System.out.println("lesser");
            }
        """.trimIndent().trim()

        // Expected AST structure for if-else statement (Python/Java)
        val expectedAst = ModuleNode(
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

        // JavaScript-specific AST (uses doubles for numbers)
        val expectedJavaScriptAst = ModuleNode(
            body = listOf(
                IfNode(
                    test = CompareNode(
                        left = NameNode(id = "x", ctx = Load),
                        op = ">",
                        right = ConstantNode(5.0)
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

        val allLanguageSetupsForIfElseTest = listOf(
            Triple(pythonConfig, pythonCode, expectedAst),
            Triple(javaScriptConfig, javascriptCode, expectedJavaScriptAst),
            Triple(javaConfig, javaCode, expectedAst)
            // To add a new language for the if-else test, add its Triple here
        )

        executeTranspilationTests("If-Else Statement", allLanguageSetupsForIfElseTest)
    }

    @Test
    fun `test simple if statement transpilation`() {
        // Simple if statement without else clause
        val pythonCode = """
            if a == 1:
                print('one')
        """.trimIndent().trim()

        val javascriptCode = """
            if (a === 1) {
                console.log('one');
            }
        """.trimIndent().trim()

        val javaCode = """
            if (a == 1) {
                System.out.println("one");
            }
        """.trimIndent().trim()

        // Expected AST structure for simple if statement
        val expectedPythonAst = ModuleNode(
            body = listOf(
                IfNode(
                    test = CompareNode(
                        left = NameNode(id = "a", ctx = Load),
                        op = "==",
                        right = ConstantNode(1)
                    ),
                    body = listOf(
                        PrintNode(expression = ConstantNode("one"))
                    ),
                    orelse = emptyList()
                )
            )
        )

        // JavaScript uses strict equality ===
        val expectedJSAst = ModuleNode(
            body = listOf(
                IfNode(
                    test = CompareNode(
                        left = NameNode(id = "a", ctx = Load),
                        op = "===",
                        right = ConstantNode(1.0) // JS numbers are doubles
                    ),
                    body = listOf(
                        PrintNode(expression = ConstantNode("one"))
                    ),
                    orelse = emptyList()
                )
            )
        )

        val allLanguageSetupsForSimpleIfTest = listOf(
            Triple(pythonConfig, pythonCode, expectedPythonAst),
            Triple(javaScriptConfig, javascriptCode, expectedJSAst),
            Triple(javaConfig, javaCode, expectedPythonAst) // Java uses == and integers like Python
            // To add a new language for the simple if test, add its Triple here
        )

        executeTranspilationTests("Simple If Statement", allLanguageSetupsForSimpleIfTest)
    }
}

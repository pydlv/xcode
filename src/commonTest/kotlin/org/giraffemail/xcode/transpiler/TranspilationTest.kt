package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.javaparser.JavaGenerator
import org.giraffemail.xcode.javaparser.JavaParser
import org.giraffemail.xcode.javascriptparser.JavaScriptGenerator
import org.giraffemail.xcode.javascriptparser.JavaScriptParser
import org.giraffemail.xcode.pythonparser.PythonGenerator
import org.giraffemail.xcode.pythonparser.PythonParser
import org.giraffemail.xcode.haskellparser.HaskellGenerator
import org.giraffemail.xcode.haskellparser.HaskellParser
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
    private val haskellConfig = LanguageConfig("Haskell", HaskellParser::parse, { ast -> HaskellGenerator().generate(ast) })

    private fun assertRoundTripTranspilation(
        originalCode: String,
        expectedIntermediateCode: String,
        lang1Config: LanguageConfig,
        lang2Config: LanguageConfig,
        expectedCommonAst: AstNode
    ) {
        println("Starting round-trip transpilation test: ${lang1Config.name} -> ${lang2Config.name} -> ${lang1Config.name}")
        println("Original ${lang1Config.name} code:\\n$originalCode")

        try {
            // 1. Lang1 to AST
            println("\\nStep 1: Parsing ${lang1Config.name} to AST...")
            val astFromLang1 = lang1Config.parseFn(originalCode)
            println("Generated AST from ${lang1Config.name}:\\n$astFromLang1")
            println("Expected common AST:\\n$expectedCommonAst")
            assertEquals(expectedCommonAst, astFromLang1, "Initial AST from ${lang1Config.name} parser is not as expected for code: \'$originalCode\'.")
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
            println("Expected common AST:\\n$expectedCommonAst")
            assertEquals(expectedCommonAst, astFromLang2, "AST from ${lang2Config.name} parser is not as expected for code: \'$generatedIntermediateCode\'.")
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
        expectedCommonAst: AstNode
    ) {
        if (languageSequence.size < 2) {
            fail("Language sequence must have at least 2 languages for sequential transpilation.")
        }
        if (expectedIntermediateGeneratedCodes.size != languageSequence.size - 1) {
            fail("Mismatch in expected intermediate codes count. Expected ${languageSequence.size - 1}, got ${expectedIntermediateGeneratedCodes.size}")
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
                assertEquals(expectedCommonAst, currentAst, "AST for ${currentLangConfig.name} (stage ${i+1}) did not match expected common AST.")

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
                    assertEquals(expectedCommonAst, finalParsedAst, "AST of final ${finalTargetLang.name} code should match expected common AST.")
                }
            }
        } catch (e: Exception) {
            fail("Sequential transpilation test $sequenceDescription -> ${languageSequence.first().name} failed: ${e.message}\\n${e.stackTraceToString()}")
        }
        println("Sequential transpilation test $sequenceDescription -> ${languageSequence.first().name} completed successfully.\\n")
    }


    private fun executeTranspilationTests(
        testName: String,
        allLanguageSetups: List<Pair<LanguageConfig, String>>,
        expectedCommonAst: AstNode
    ) {
        val nLanguages = allLanguageSetups.size
        if (nLanguages >= 2) {
            println("\\\\n--- Starting Pairwise Round-Trip Tests for \'$testName\' (n=${nLanguages}) ---")
            for (i in allLanguageSetups.indices) {
                for (j in allLanguageSetups.indices) {
                    if (i == j) continue

                    val (lang1Config, code1) = allLanguageSetups[i]
                    val (lang2Config, code2) = allLanguageSetups[j]

                    assertRoundTripTranspilation(
                        originalCode = code1,
                        expectedIntermediateCode = code2,
                        lang1Config = lang1Config,
                        lang2Config = lang2Config,
                        expectedCommonAst = expectedCommonAst
                    )
                }
            }
            println("\\\\n--- Pairwise Round-Trip Tests for \'$testName\' Completed ---")

            println("\\\\n--- Starting Sequential Transpilation Tests for \'$testName\' (n=${nLanguages}) ---")
            for (startIndex in 0 until nLanguages) {
                val currentLanguageSequence = mutableListOf<LanguageConfig>()
                val currentExpectedIntermediateCodes = mutableListOf<String>()

                for (offset in 0 until nLanguages) {
                    val currentIndex = (startIndex + offset) % nLanguages
                    val (langConfig, _) = allLanguageSetups[currentIndex]
                    currentLanguageSequence.add(langConfig)
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
                    expectedCommonAst = expectedCommonAst
                )
            }
            println("\\\\n--- Sequential Transpilation Tests for \'$testName\' (n=${nLanguages}) Completed ---")
        } else if (nLanguages == 1) {
            val (config, code) = allLanguageSetups.first()
            println("Testing single language parse/generate for \'$testName\': ${config.name}")
            try {
                val parsed = config.parseFn(code)
                assertEquals(expectedCommonAst, parsed, "AST mismatch for single language parse ($testName).")
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
        val haskellPrintCode = "putStrLn \"cookies\""

        // Corrected expected AST for Java to match the others for this simple print case.
        // The Java parser should produce a ConstantNode with a string value, identical to Python/JS.
        val commonExpectedPrintAst = ModuleNode(
            body = listOf(PrintNode(expression = ConstantNode("cookies")))
        )

        val allLanguageSetupsForPrintTest = listOf(
            Pair(pythonConfig, pythonPrintCode),
            Pair(javaScriptConfig, jsPrintCode),
            Pair(javaConfig, javaPrintCode),
            Pair(haskellConfig, haskellPrintCode)
            // To add a new language for the print test, add its Pair here
        )

        executeTranspilationTests("Print Cookies", allLanguageSetupsForPrintTest, commonExpectedPrintAst)
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

        // Define code snippets
        val pythonCodeAdd = "print(1 + 2)"
        val jsCodeAdd = "console.log(1 + 2);"
        val javaCodeAdd = "System.out.println(1 + 2);"
        val haskellCodeAdd = "putStrLn (1 + 2)"

        // Create language setups list - all use common AST now due to normalization
        val allLanguageSetupsForPrintAddTest = listOf(
            Pair(pythonConfig, pythonCodeAdd),
            Pair(javaScriptConfig, jsCodeAdd),
            Pair(javaConfig, javaCodeAdd),
            Pair(haskellConfig, haskellCodeAdd)
            // To add a new language for the print with addition test, add its Pair here
        )

        executeTranspilationTests("Print Addition", allLanguageSetupsForPrintAddTest, commonAstAdd)
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

        // Java code for recursive fibonacci
        val javaCode = """public static void fib(Object a, Object b) {
        c = a + b;
        System.out.println(c);
        fib(b, c);
    }
fib(0, 1);"""

        // Haskell code for recursive fibonacci
        val haskellCode = """fib a b = let c = a + b in putStrLn c
fib 0 1"""

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
        val expectedCommonAst = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "fib",
                    args = listOf(NameNode(id = "a", ctx = Param), NameNode(id = "b", ctx = Param)),
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

        val allLanguageSetupsForFibonacciTest = listOf(
            Pair(pythonConfig, pythonCode),
            Pair(javaScriptConfig, javascriptCode),
            Pair(javaConfig, javaCode)
            // TODO: Fix Haskell fibonacci implementation 
            // Pair(haskellConfig, haskellCode)
            // To add a new language for the fibonacci test, add its Pair here
        )

        executeTranspilationTests("Recursive Fibonacci", allLanguageSetupsForFibonacciTest, expectedCommonAst)
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

        val haskellCode = """if x > 5 then putStrLn "greater" else putStrLn "lesser""""

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

        val allLanguageSetupsForIfElseTest = listOf(
            Pair(pythonConfig, pythonCode),
            Pair(javaScriptConfig, javascriptCode),
            Pair(javaConfig, javaCode)
            // TODO: Fix Haskell if-else parsing issues
            // Pair(haskellConfig, haskellCode)
            // To add a new language for the if-else test, add its Pair here
        )

        executeTranspilationTests("If-Else Statement", allLanguageSetupsForIfElseTest, expectedAst)
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

        val haskellCode = """if a == 1 then putStrLn "one" else undefined"""

        // Expected common AST structure for simple if statement
        // All languages should produce this same AST due to normalization
        val expectedCommonAst = ModuleNode(
            body = listOf(
                IfNode(
                    test = CompareNode(
                        left = NameNode(id = "a", ctx = Load),
                        op = "==", // Canonical equality operator
                        right = ConstantNode(1) // Use integer for consistency
                    ),
                    body = listOf(
                        PrintNode(expression = ConstantNode("one"))
                    ),
                    orelse = emptyList()
                )
            )
        )

        val allLanguageSetupsForSimpleIfTest = listOf(
            Pair(pythonConfig, pythonCode),
            Pair(javaScriptConfig, javascriptCode),
            Pair(javaConfig, javaCode)
            // TODO: Fix Haskell if parsing issues (similar to if-else)
            // Pair(haskellConfig, haskellCode)
            // To add a new language for the simple if test, add its Pair here
        )

        executeTranspilationTests("Simple If Statement", allLanguageSetupsForSimpleIfTest, expectedCommonAst)
    }
}

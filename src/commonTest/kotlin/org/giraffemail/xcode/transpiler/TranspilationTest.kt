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

    private fun assertRoundTripTranspilation(
        originalCode: String,
        expectedIntermediateCode: String,
        lang1Config: LanguageConfig,
        lang2Config: LanguageConfig,
        expectedInitialAst: AstNode? = null,      // Expected AST from original code (lang1)
        expectedIntermediateAst: AstNode? = null  // Expected AST from intermediate code (lang2) after generation and re-parsing
    ) {
        try {
            // 1. Lang1 to AST
            val astFromLang1 = lang1Config.parse(originalCode)
            expectedInitialAst?.let {
                assertEquals(it, astFromLang1, "Initial AST from ${lang1Config.name} parser is not as expected for code: '$originalCode'.")
            }

            // 2. AST to Lang2
            val generatedIntermediateCode = lang2Config.generate(astFromLang1)
            assertEquals(expectedIntermediateCode, generatedIntermediateCode, "${lang1Config.name} AST to ${lang2Config.name} code generation failed.")

            // 3. Lang2 to AST
            val astFromLang2 = lang2Config.parse(generatedIntermediateCode)
            expectedIntermediateAst?.let {
                assertEquals(it, astFromLang2, "AST from ${lang2Config.name} parser is not as expected for code: '$generatedIntermediateCode'.")
            }

            // 4. AST to Lang1 (back to original)
            val finalOriginalCode = lang1Config.generate(astFromLang2)
            assertEquals(originalCode, finalOriginalCode, "${lang2Config.name} AST to ${lang1Config.name} code generation failed (round trip).")

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
        val expectedPyAst = ModuleNode(body=listOf(ExprNode(value=CallNode(func=NameNode("print", Load), args=listOf(ConstantNode("cookies"))))))
        val expectedJsAstAfterRoundtrip = ModuleNode(body=listOf(ExprNode(value=CallNode(func=MemberExpressionNode(NameNode("console", Load), NameNode("log", Load)), args=listOf(ConstantNode("cookies"))))))

        assertRoundTripTranspilation(
            originalCode = originalPythonCode,
            expectedIntermediateCode = expectedIntermediateJsCode,
            lang1Config = LanguageConfig("Python", PythonParser::parse, PythonGenerator::generate),
            lang2Config = LanguageConfig("JavaScript", JavaScriptParser::parse, JavaScriptGenerator::generate),
            expectedInitialAst = expectedPyAst,
            expectedIntermediateAst = expectedJsAstAfterRoundtrip
        )
    }

    @Test
    fun `test javascript to python and back to javascript for console log statement`() {
        val originalJsCode = "console.log('more_cookies');"
        val expectedIntermediatePythonCode = "print('more_cookies')"
        val expectedJsAst = ModuleNode(body=listOf(ExprNode(value=CallNode(func=MemberExpressionNode(NameNode("console", Load), NameNode("log", Load)), args=listOf(ConstantNode("more_cookies"))))))
        val expectedPyAstAfterRoundtrip = ModuleNode(body=listOf(ExprNode(value=CallNode(func=NameNode("print", Load), args=listOf(ConstantNode("more_cookies"))))))

        assertRoundTripTranspilation(
            originalCode = originalJsCode,
            expectedIntermediateCode = expectedIntermediatePythonCode,
            lang1Config = LanguageConfig("JavaScript", JavaScriptParser::parse, JavaScriptGenerator::generate),
            lang2Config = LanguageConfig("Python", PythonParser::parse, PythonGenerator::generate),
            expectedInitialAst = expectedJsAst,
            expectedIntermediateAst = expectedPyAstAfterRoundtrip
        )
    }

    @Test
    fun `test python to javascript and back to python for print with addition`() {
        val originalPythonCode = "print(1 + 2)"
        val expectedIntermediateJsCode = "console.log(1 + 2);"
        val expectedPyAst = ModuleNode(body=listOf(ExprNode(value=CallNode(func=NameNode("print", Load), args=listOf(BinaryOpNode(ConstantNode(1), "+", ConstantNode(2)))))))
        val expectedJsAstAfterRoundtrip = ModuleNode(body=listOf(ExprNode(value=CallNode(func=MemberExpressionNode(NameNode("console", Load), NameNode("log", Load)), args=listOf(BinaryOpNode(ConstantNode(1), "+", ConstantNode(2)))))))

        assertRoundTripTranspilation(
            originalCode = originalPythonCode,
            expectedIntermediateCode = expectedIntermediateJsCode,
            lang1Config = LanguageConfig("Python", PythonParser::parse, PythonGenerator::generate),
            lang2Config = LanguageConfig("JavaScript", JavaScriptParser::parse, JavaScriptGenerator::generate),
            expectedInitialAst = expectedPyAst,
            expectedIntermediateAst = expectedJsAstAfterRoundtrip
        )
    }

    @Test
    fun `test javascript to python and back to javascript for console log with addition`() {
        val originalJsCode = "console.log(1 + 2);"
        val expectedIntermediatePythonCode = "print(1 + 2)"
        val expectedInitialJsAst = ModuleNode(body=listOf(ExprNode(value=CallNode(func=MemberExpressionNode(NameNode("console", Load), NameNode("log", Load)), args=listOf(BinaryOpNode(ConstantNode(1), "+", ConstantNode(2)))))))
        val expectedPyAstAfterRoundtrip = ModuleNode(body=listOf(ExprNode(value=CallNode(func=NameNode("print", Load), args=listOf(BinaryOpNode(ConstantNode(1), "+", ConstantNode(2)))))))

        assertRoundTripTranspilation(
            originalCode = originalJsCode,
            expectedIntermediateCode = expectedIntermediatePythonCode,
            lang1Config = LanguageConfig("JavaScript", JavaScriptParser::parse, JavaScriptGenerator::generate),
            lang2Config = LanguageConfig("Python", PythonParser::parse, PythonGenerator::generate),
            expectedInitialAst = expectedInitialJsAst,
            expectedIntermediateAst = expectedPyAstAfterRoundtrip
        )
    }
}

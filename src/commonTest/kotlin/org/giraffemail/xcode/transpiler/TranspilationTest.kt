package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.pythonparser.PythonParser
import org.giraffemail.xcode.pythonparser.PythonGenerator
import org.giraffemail.xcode.javascriptparser.JavaScriptParser
import org.giraffemail.xcode.javascriptparser.JavaScriptGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

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
        parseLang1: (String) -> AstNode,
        generateLang1: (AstNode) -> String,
        parseLang2: (String) -> AstNode,
        generateLang2: (AstNode) -> String,
        lang1Name: String,
        lang2Name: String,
        expectedInitialAst: AstNode? = null,      // Expected AST from original code (lang1)
        expectedIntermediateAst: AstNode? = null  // Expected AST from intermediate code (lang2) after generation and re-parsing
    ) {
        try {
            // 1. Lang1 to AST
            val astFromLang1 = parseLang1(originalCode)
            expectedInitialAst?.let {
                assertEquals(it, astFromLang1, "Initial AST from $lang1Name parser is not as expected for code: '$originalCode'.")
            }

            // 2. AST to Lang2
            val generatedIntermediateCode = generateLang2(astFromLang1)
            assertEquals(expectedIntermediateCode, generatedIntermediateCode, "$lang1Name AST to $lang2Name code generation failed.")

            // 3. Lang2 to AST
            val astFromLang2 = parseLang2(generatedIntermediateCode)
            expectedIntermediateAst?.let {
                assertEquals(it, astFromLang2, "AST from $lang2Name parser is not as expected for code: '$generatedIntermediateCode'.")
            }

            // 4. AST to Lang1 (back to original)
            val finalOriginalCode = generateLang1(astFromLang2)
            assertEquals(originalCode, finalOriginalCode, "$lang2Name AST to $lang1Name code generation failed (round trip).")

        } catch (e: AstParseException) {
            fail("Transpilation test ($lang1Name to $lang2Name to $lang1Name for code '$originalCode') failed due to parsing error: ${e.message}", e)
        } catch (e: Exception) {
            fail("Transpilation test ($lang1Name to $lang2Name to $lang1Name for code '$originalCode') failed due to an unexpected error: ${e.message}", e)
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
            parseLang1 = PythonParser::parse,
            generateLang1 = PythonGenerator::generate,
            parseLang2 = JavaScriptParser::parse,
            generateLang2 = JavaScriptGenerator::generate,
            lang1Name = "Python",
            lang2Name = "JavaScript",
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
            parseLang1 = JavaScriptParser::parse,
            generateLang1 = JavaScriptGenerator::generate,
            parseLang2 = PythonParser::parse,
            generateLang2 = PythonGenerator::generate,
            lang1Name = "JavaScript",
            lang2Name = "Python",
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
            parseLang1 = PythonParser::parse,
            generateLang1 = PythonGenerator::generate,
            parseLang2 = JavaScriptParser::parse,
            generateLang2 = JavaScriptGenerator::generate,
            lang1Name = "Python",
            lang2Name = "JavaScript",
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
            parseLang1 = JavaScriptParser::parse,
            generateLang1 = JavaScriptGenerator::generate,
            parseLang2 = PythonParser::parse,
            generateLang2 = PythonGenerator::generate,
            lang1Name = "JavaScript",
            lang2Name = "Python",
            expectedInitialAst = expectedInitialJsAst,
            expectedIntermediateAst = expectedPyAstAfterRoundtrip
        )
    }
}

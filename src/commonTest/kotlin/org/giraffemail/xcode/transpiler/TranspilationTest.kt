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

    @Test
    fun `test python to javascript and back to python for print statement`() {
        val originalPythonCode = "print('cookies')"
        val expectedIntermediateJsCode = "console.log('cookies');"

        try {
            // 1. Python to AST
            val astFromPython = PythonParser.parse(originalPythonCode)

            // 2. AST to JavaScript
            val generatedJsCode = JavaScriptGenerator.generate(astFromPython)
            assertEquals(expectedIntermediateJsCode, generatedJsCode, "Python AST to JS code generation failed.")

            // 3. JavaScript to AST
            val astFromJs = JavaScriptParser.parse(generatedJsCode)

            // 4. AST to Python (back to original)
            val finalPythonCode = PythonGenerator.generate(astFromJs)
            assertEquals(originalPythonCode, finalPythonCode, "JS AST to Python code generation failed (round trip).")

        } catch (e: AstParseException) {
            fail("Transpilation test failed due to parsing error: ${e.message}", e)
        } catch (e: Exception) {
            fail("Transpilation test failed due to an unexpected error: ${e.message}", e)
        }
    }

    @Test
    fun `test javascript to python and back to javascript for console log statement`() {
        val originalJsCode = "console.log('more_cookies');" // Using a different string to ensure no test interference
        val expectedIntermediatePythonCode = "print('more_cookies')"

        try {
            // 1. JavaScript to AST
            val astFromJs = JavaScriptParser.parse(originalJsCode)

            // 2. AST to Python
            val generatedPythonCode = PythonGenerator.generate(astFromJs)
            assertEquals(expectedIntermediatePythonCode, generatedPythonCode, "JS AST to Python code generation failed.")

            // 3. Python to AST
            val astFromPython = PythonParser.parse(generatedPythonCode)

            // 4. AST to JavaScript (back to original)
            val finalJsCode = JavaScriptGenerator.generate(astFromPython)
            assertEquals(originalJsCode, finalJsCode, "Python AST to JS code generation failed (round trip).")

        } catch (e: AstParseException) {
            fail("Transpilation test (JS to Python) failed due to parsing error: ${e.message}", e)
        } catch (e: Exception) {
            fail("Transpilation test (JS to Python) failed due to an unexpected error: ${e.message}", e)
        }
    }

    @Test
    fun `test python to javascript and back to python for print with addition`() {
        val originalPythonCode = "print(1 + 2)"
        val expectedIntermediateJsCode = "console.log(1 + 2);"

        try {
            // 1. Python to AST
            val astFromPython = PythonParser.parse(originalPythonCode)
            // Verify the AST from Python is what we expect (optional, but good for debugging)
            val expectedPyAst = ModuleNode(body=listOf(ExprNode(value=CallNode(func=NameNode("print", Load), args=listOf(BinaryOpNode(ConstantNode(1), "+", ConstantNode(2)))))))
            assertEquals(expectedPyAst, astFromPython, "Initial AST from Python parser is not as expected.")

            // 2. AST to JavaScript
            val generatedJsCode = JavaScriptGenerator.generate(astFromPython)
            assertEquals(expectedIntermediateJsCode, generatedJsCode, "Python AST to JS code generation failed for addition.")

            // 3. JavaScript to AST
            val astFromJs = JavaScriptParser.parse(generatedJsCode)
            // Verify the AST from JS is what we expect (optional)
            val expectedJsAst = ModuleNode(body=listOf(ExprNode(value=CallNode(func=MemberExpressionNode(NameNode("console", Load), NameNode("log", Load)), args=listOf(BinaryOpNode(ConstantNode(1), "+", ConstantNode(2)))))))
            assertEquals(expectedJsAst, astFromJs, "AST from JavaScript parser is not as expected after JS generation.")

            // 4. AST to Python (back to original)
            val finalPythonCode = PythonGenerator.generate(astFromJs)
            assertEquals(originalPythonCode, finalPythonCode, "JS AST to Python code generation failed for addition (round trip).")

        } catch (e: AstParseException) {
            fail("Transpilation test (Python to JS for addition) failed due to parsing error: ${e.message}", e)
        } catch (e: Exception) {
            fail("Transpilation test (Python to JS for addition) failed due to an unexpected error: ${e.message}", e)
        }
    }

    @Test
    fun `test javascript to python and back to javascript for console log with addition`() {
        val originalJsCode = "console.log(1 + 2);"
        val expectedIntermediatePythonCode = "print(1 + 2)"

        try {
            // 1. JavaScript to AST
            val astFromJs = JavaScriptParser.parse(originalJsCode)
            // Verify the AST from JS is what we expect (optional, but good for debugging)
            val expectedInitialJsAst = ModuleNode(body=listOf(ExprNode(value=CallNode(func=MemberExpressionNode(NameNode("console", Load), NameNode("log", Load)), args=listOf(BinaryOpNode(ConstantNode(1), "+", ConstantNode(2)))))))
            assertEquals(expectedInitialJsAst, astFromJs, "Initial AST from JavaScript parser is not as expected.")

            // 2. AST to Python
            val generatedPythonCode = PythonGenerator.generate(astFromJs)
            assertEquals(expectedIntermediatePythonCode, generatedPythonCode, "JS AST to Python code generation failed for addition.")

            // 3. Python to AST
            val astFromPython = PythonParser.parse(generatedPythonCode)
            // Verify the AST from Python is what we expect (optional)
            val expectedPyAst = ModuleNode(body=listOf(ExprNode(value=CallNode(func=NameNode("print", Load), args=listOf(BinaryOpNode(ConstantNode(1), "+", ConstantNode(2)))))))
            assertEquals(expectedPyAst, astFromPython, "AST from Python parser is not as expected after Python generation.")

            // 4. AST to JavaScript (back to original)
            val finalJsCode = JavaScriptGenerator.generate(astFromPython)
            assertEquals(originalJsCode, finalJsCode, "Python AST to JS code generation failed for addition (round trip).")

        } catch (e: AstParseException) {
            fail("Transpilation test (JS to Python for addition) failed due to parsing error: ${e.message}", e)
        } catch (e: Exception) {
            fail("Transpilation test (JS to Python for addition) failed due to an unexpected error: ${e.message}", e)
        }
    }
}

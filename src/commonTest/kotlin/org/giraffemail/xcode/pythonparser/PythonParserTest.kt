package org.giraffemail.xcode.pythonparser

import org.giraffemail.xcode.ast.* // Import common AST nodes
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.test.assertEquals

class PythonParserTest {
    @Test
    fun `test parsing specific input that triggers error - placeholder`() {
        val invalidPythonCode = "trigger_error_python" // Specific string to trigger placeholder error for Python
        try {
            PythonParser.parseWithMetadata(invalidPythonCode, emptyList())
            fail("Parsing 'trigger_error_python' should have thrown AstParseException (placeholder test)")
        } catch (e: AstParseException) { // Changed to AstParseException
            // Expected exception from placeholder logic
            assertNotNull(e.message, "Exception message should not be null")
            assertTrue(e.message!!.contains("Simulated parsing error for 'trigger_error_python' input in Python."), "Incorrect error message: ${e.message}") // Updated expected message
            println("Caught expected placeholder exception: ${e.message}")
        } catch (e: Exception) {
            fail("Unexpected exception type: ${e::class.simpleName} - ${e.message}")
        }
    }

    @Test
    fun `test parsing empty string - expects minimal AST`() {
         val emptyPythonCode = ""
         // Expected AST uses common data classes (already correct from previous refactor)
         val expectedAst = ModuleNode(body = emptyList())
         try {
             val ast = PythonParser.parseWithMetadata(emptyPythonCode, emptyList())
             assertNotNull(ast, "AST should not be null for empty string")
             assertEquals(expectedAst, ast, "AST for empty string did not match. \\nActual: $ast\\nExpected: $expectedAst")
         } catch (e: AstParseException) { // Changed to AstParseException
             fail("Parsing empty string failed (placeholder test): ${e.message}", e)
         }
    }

    @Test
    fun `test parsing hello world program - expects specific AST`() {
        val pythonCode = "print('Hello, World!')"

        // Adjusted expected AST structure
        val expectedAst = ModuleNode(
            body = listOf(
                PrintNode( // ANTLR parser creates PrintNode directly
                    expression = ConstantNode(value = "Hello, World!")
                )
            )
        )

        try {
            val ast = PythonParser.parseWithMetadata(pythonCode, emptyList())
            assertNotNull(ast, "AST should not be null")
            assertEquals(expectedAst, ast, "AST did not match expected structure. \\nActual: $ast\\nExpected: $expectedAst")
        } catch (e: AstParseException) { // Changed to AstParseException
            fail("Parsing failed for hello world: ${e.message}", e)
        }
    }

    @Test
    fun `test parsing print with arbitrary string`() {
        val customString = "Python is powerful!"
        val pythonCode = "print('$customString')"

        // Adjusted expected AST structure
        val expectedAst = ModuleNode(
            body = listOf(
                PrintNode( // ANTLR parser creates PrintNode directly
                    expression = ConstantNode(value = customString)
                )
            )
        )

        try {
            val ast = PythonParser.parseWithMetadata(pythonCode, emptyList())
            assertNotNull(ast, "AST should not be null")
            assertEquals(expectedAst, ast, "AST did not match expected structure for arbitrary string. \nActual: $ast\nExpected: $expectedAst")
        } catch (e: AstParseException) {
            fail("Parsing failed for print with arbitrary string: ${e.message}", e)
        }
    }

    @Test
    fun `test parsing print with simple addition`() {
        val pythonCode = "print(1 + 2)"
        // Adjusted expected AST structure
        val expectedAst = ModuleNode(
            body = listOf(
                PrintNode( // ANTLR parser creates PrintNode directly
                    expression = BinaryOpNode( // This will require grammar change
                        left = ConstantNode(value = 1, typeInfo = CanonicalTypes.Number),
                        op = "+",
                        right = ConstantNode(value = 2, typeInfo = CanonicalTypes.Number)
                    )
                )
            )
        )

        try {
            val ast = PythonParser.parseWithMetadata(pythonCode, emptyList())
            assertEquals(expectedAst, ast, "AST for print with addition did not match expected.")
        } catch (e: AstParseException) {
            fail("Parsing failed for print with addition: ${e.message}", e)
        }
    }

    @Test
    fun `test parsing fibonacci call with integer arguments`() {
        val pythonCode = "fib(0, 1)"
        val expectedAst = ModuleNode(
            body = listOf(
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "fib", ctx = Load),
                        args = listOf(
                            ConstantNode(value = 0, typeInfo = CanonicalTypes.Number),
                            ConstantNode(value = 1, typeInfo = CanonicalTypes.Number)
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )

        try {
            val ast = PythonParser.parseWithMetadata(pythonCode, emptyList())
            assertEquals(expectedAst, ast, "AST for fib(0, 1) did not match expected.")
        } catch (e: AstParseException) {
            fail("Parsing failed for fib(0, 1): ${e.message}", e)
        }
    }
}

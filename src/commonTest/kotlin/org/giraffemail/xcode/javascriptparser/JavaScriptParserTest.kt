package org.giraffemail.xcode.javascriptparser

import org.giraffemail.xcode.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class JavaScriptParserTest {

    @Test
    fun `test parsing console log hello world`() {
        val jsCode = "console.log('Hello, World!');"

        // Adjusted expected AST structure
        val expectedAst = ModuleNode(
            body = listOf(
                PrintNode( // ANTLR parser creates PrintNode directly
                    expression = ConstantNode(value = "Hello, World!")
                )
            )
        )

        try {
            val ast = JavaScriptParser.parseWithMetadata(jsCode, emptyList()) // This will initially fail as JavaScriptParser doesn't exist
            assertNotNull(ast, "AST should not be null")
            assertEquals(expectedAst, ast, "AST did not match expected structure. \nActual: $ast\nExpected: $expectedAst")
        } catch (e: AstParseException) {
            fail("Parsing failed for console.log: ${e.message}", e)
        }
    }

    @Test
    fun `test parsing console log with arbitrary string`() {
        val customString = "JavaScript is fun!"
        val jsCode = "console.log('$customString');"

        // Adjusted expected AST structure
        val expectedAst = ModuleNode(
            body = listOf(
                PrintNode( // ANTLR parser creates PrintNode directly
                    expression = ConstantNode(value = customString)
                )
            )
        )

        try {
            val ast = JavaScriptParser.parseWithMetadata(jsCode, emptyList())
            assertNotNull(ast, "AST should not be null")
            assertEquals(expectedAst, ast, "AST did not match expected structure for arbitrary string. \nActual: $ast\nExpected: $expectedAst")
        } catch (e: AstParseException) {
            fail("Parsing failed for console.log with arbitrary string: ${e.message}", e)
        }
    }

    @Test
    fun `test parsing console log with simple addition`() {
        val jsCode = "console.log(1 + 2);"
        // Expected AST structure - normalized to integers for common AST
        val expectedAst = ModuleNode(
            body = listOf(
                PrintNode( // ANTLR parser creates PrintNode directly
                    expression = BinaryOpNode( // This will require grammar change for JS
                        left = ConstantNode(value = 1), // Normalized to integer
                        op = "+",
                        right = ConstantNode(value = 2) // Normalized to integer
                    )
                )
            )
        )

        try {
            val ast = JavaScriptParser.parseWithMetadata(jsCode, emptyList())
            assertEquals(expectedAst, ast, "AST for console.log with addition did not match expected.")
        } catch (e: AstParseException) {
            fail("Parsing failed for console.log with addition: ${e.message}", e)
        }
    }

    @Test
    fun `test parsing fibonacci call with numeric arguments`() {
        val jsCode = "fib(0, 1);"
        val expectedAst = ModuleNode(
            body = listOf(
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "fib", ctx = Load),
                        args = listOf(
                            ConstantNode(value = 0), // Normalized to integer
                            ConstantNode(value = 1)  // Normalized to integer
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )

        try {
            val ast = JavaScriptParser.parseWithMetadata(jsCode, emptyList())
            assertEquals(expectedAst, ast, "AST for fib(0, 1) did not match expected.")
        } catch (e: AstParseException) {
            fail("Parsing failed for fib(0, 1): ${e.message}", e)
        }
    }
}

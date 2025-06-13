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
            val ast = JavaScriptParser.parse(jsCode) // This will initially fail as JavaScriptParser doesn't exist
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
            val ast = JavaScriptParser.parse(jsCode)
            assertNotNull(ast, "AST should not be null")
            assertEquals(expectedAst, ast, "AST did not match expected structure for arbitrary string. \nActual: $ast\nExpected: $expectedAst")
        } catch (e: AstParseException) {
            fail("Parsing failed for console.log with arbitrary string: ${e.message}", e)
        }
    }

    @Test
    fun `test parsing console log with simple addition`() {
        val jsCode = "console.log(1 + 2);"
        // Adjusted expected AST structure
        val expectedAst = ModuleNode(
            body = listOf(
                PrintNode( // ANTLR parser creates PrintNode directly
                    expression = BinaryOpNode( // This will require grammar change for JS
                        left = ConstantNode(value = 1),
                        op = "+",
                        right = ConstantNode(value = 2)
                    )
                )
            )
        )

        try {
            val ast = JavaScriptParser.parse(jsCode)
            assertEquals(expectedAst, ast, "AST for console.log with addition did not match expected.")
        } catch (e: AstParseException) {
            fail("Parsing failed for console.log with addition: ${e.message}", e)
        }
    }
}

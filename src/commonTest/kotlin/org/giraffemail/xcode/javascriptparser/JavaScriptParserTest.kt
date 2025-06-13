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

        val expectedAst = ModuleNode(
            body = listOf(
                ExprNode(
                    value = CallNode(
                        func = MemberExpressionNode(
                            obj = NameNode(id = "console", ctx = Load),
                            property = NameNode(id = "log", ctx = Load) // Assuming 'log' is also loaded as a name/identifier here
                        ),
                        args = listOf(
                            ConstantNode(value = "Hello, World!")
                        ),
                        keywords = emptyList()
                    )
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
}


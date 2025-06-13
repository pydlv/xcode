package org.giraffemail.xcode.javascriptparser

import org.giraffemail.xcode.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaScriptGeneratorTest {

    @Test
    fun `test generating console log hello world`() {
        val ast = ModuleNode(
            body = listOf(
                ExprNode(
                    value = CallNode(
                        func = MemberExpressionNode(
                            obj = NameNode(id = "console", ctx = Load),
                            property = NameNode(id = "log", ctx = Load)
                        ),
                        args = listOf(
                            ConstantNode(value = "Hello, World!")
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )
        val expectedCode = "console.log('Hello, World!');"
        val actualCode = JavaScriptGenerator.generate(ast)
        assertEquals(expectedCode, actualCode, "Generated JavaScript code did not match expected.")
    }

    @Test
    fun `test generating console log with arbitrary string`() {
        val customString = "JavaScript generates this too!"
        val ast = ModuleNode(
            body = listOf(
                ExprNode(
                    value = CallNode(
                        func = MemberExpressionNode(
                            obj = NameNode(id = "console", ctx = Load),
                            property = NameNode(id = "log", ctx = Load)
                        ),
                        args = listOf(
                            ConstantNode(value = customString)
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )
        val expectedCode = "console.log('$customString');"
        val actualCode = JavaScriptGenerator.generate(ast)
        assertEquals(expectedCode, actualCode, "Generated JavaScript code with arbitrary string did not match expected.")
    }
}

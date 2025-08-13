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
        val actualCode = JavaScriptGenerator().generateWithNativeMetadata(ast).code
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
        val actualCode = JavaScriptGenerator().generateWithNativeMetadata(ast).code
        assertEquals(expectedCode, actualCode, "Generated JavaScript code with arbitrary string did not match expected.")
    }

    @Test
    fun `test generating simple addition expression`() {
        val ast = BinaryOpNode(
            left = ConstantNode(value = 1),
            op = "+",
            right = ConstantNode(value = 2)
        )
        val moduleAst = ModuleNode(body = listOf(ExprNode(value = ast)))
        // For JavaScript, an expression statement ends with a semicolon.
        val expectedCode = "1 + 2;"
        val actualCode = JavaScriptGenerator().generateWithNativeMetadata(moduleAst).code
        assertEquals(expectedCode, actualCode, "Generated JavaScript code for simple addition did not match expected.")
    }

    @Test
    fun `test generating if statement without else`() {
        val ast = ModuleNode(
            body = listOf(
                IfNode(
                    test = CompareNode(
                        left = NameNode(id = "x", ctx = Load),
                        op = "==",
                        right = ConstantNode(value = 5)
                    ),
                    body = listOf(
                        ExprNode(
                            value = CallNode(
                                func = MemberExpressionNode(
                                    obj = NameNode(id = "console", ctx = Load),
                                    property = NameNode(id = "log", ctx = Load)
                                ),
                                args = listOf(ConstantNode(value = "x is 5")),
                                keywords = emptyList()
                            )
                        )
                    ),
                    orelse = emptyList()
                )
            )
        )
        val expectedCode = "if (x === 5) {\n    console.log('x is 5');\n}"
        val actualCode = JavaScriptGenerator().generateWithNativeMetadata(ast).code
        assertEquals(expectedCode, actualCode, "Generated JavaScript if statement without else did not match expected.")
    }

    @Test
    fun `test generating if statement with else`() {
        val ast = ModuleNode(
            body = listOf(
                IfNode(
                    test = CompareNode(
                        left = NameNode(id = "x", ctx = Load),
                        op = "==",
                        right = ConstantNode(value = 5)
                    ),
                    body = listOf(
                        ExprNode(
                            value = CallNode(
                                func = MemberExpressionNode(
                                    obj = NameNode(id = "console", ctx = Load),
                                    property = NameNode(id = "log", ctx = Load)
                                ),
                                args = listOf(ConstantNode(value = "x is 5")),
                                keywords = emptyList()
                            )
                        )
                    ),
                    orelse = listOf(
                        ExprNode(
                            value = CallNode(
                                func = MemberExpressionNode(
                                    obj = NameNode(id = "console", ctx = Load),
                                    property = NameNode(id = "log", ctx = Load)
                                ),
                                args = listOf(ConstantNode(value = "x is not 5")),
                                keywords = emptyList()
                            )
                        )
                    )
                )
            )
        )
        val expectedCode = "if (x === 5) {\n    console.log('x is 5');\n} else {\n    console.log('x is not 5');\n}"
        val actualCode = JavaScriptGenerator().generateWithNativeMetadata(ast).code
        assertEquals(expectedCode, actualCode, "Generated JavaScript if statement with else did not match expected.")
    }
}

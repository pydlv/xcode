package org.giraffemail.xcode.pythonparser

import org.giraffemail.xcode.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PythonGeneratorTest {

    @Test
    fun `test generating print hello world`() {
        val ast = ModuleNode(
            body = listOf(
                ExprNode(
                    value = CallNode(
                        func = NameNode(id = "print", ctx = Load),
                        args = listOf(
                            ConstantNode(value = "Hello, World!")
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )
        val expectedCode = "print('Hello, World!')"
        val actualCode = PythonGenerator.generate(ast)
        assertEquals(expectedCode, actualCode, "Generated Python code did not match expected.")
    }

    @Test
    fun `test generating print with arbitrary string`() {
        val customString = "Python prints this nicely!"
        val ast = ModuleNode(
            body = listOf(
                ExprNode(
                    value = CallNode(
                        func = NameNode(id = "print", ctx = Load),
                        args = listOf(
                            ConstantNode(value = customString)
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )
        val expectedCode = "print('$customString')"
        val actualCode = PythonGenerator.generate(ast)
        assertEquals(expectedCode, actualCode, "Generated Python code with arbitrary string did not match expected.")
    }
}

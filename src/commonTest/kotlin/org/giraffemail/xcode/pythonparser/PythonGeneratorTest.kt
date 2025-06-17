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
        val actualCode = PythonGenerator().generateWithMetadata(ast).code // Changed: Instantiate PythonGenerator
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
        val actualCode = PythonGenerator().generateWithMetadata(ast).code // Changed: Instantiate PythonGenerator
        assertEquals(expectedCode, actualCode, "Generated Python code with arbitrary string did not match expected.")
    }

    @Test
    fun `test generating print with simple addition`() {
        val ast = ModuleNode(
            body = listOf(
                PrintNode(
                    expression = BinaryOpNode(
                        left = ConstantNode(value = 1),
                        op = "+",
                        right = ConstantNode(value = 2)
                    )
                )
            )
        )
        val expectedCode = "print(1 + 2)"
        val actualCode = PythonGenerator().generateWithMetadata(ast).code // Changed: Instantiate PythonGenerator
        assertEquals(expectedCode, actualCode, "Generated Python code for print with simple addition did not match expected.")
    }

    @Test
    fun `test generating simple addition expression`() {
        val ast = BinaryOpNode(
            left = ConstantNode(value = 1),
            op = "+",
            right = ConstantNode(value = 2)
        )
        val moduleAst = ModuleNode(body = listOf(ExprNode(value = ast)))
        val expectedCode = "1 + 2"
        val actualCode = PythonGenerator().generateWithMetadata(moduleAst).code // Changed: Instantiate PythonGenerator
        assertEquals(expectedCode, actualCode, "Generated Python code for simple addition did not match expected.")
    }

    @Test
    fun `test generating fibonacci call with integer arguments from integer constants`() {
        val ast = ModuleNode(
            body = listOf(
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "fib", ctx = Load),
                        args = listOf(
                            ConstantNode(value = 0),
                            ConstantNode(value = 1)
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )
        val expectedCode = "fib(0, 1)"
        val actualCode = PythonGenerator().generateWithMetadata(ast).code // Changed: Instantiate PythonGenerator
        assertEquals(expectedCode, actualCode, "Generated Python code for fib(0, 1) with integer constants did not match expected.")
    }

    @Test
    fun `test generating fibonacci call with integer arguments from double constants`() {
        // This test simulates the case where the AST might have Doubles (e.g., from JavaScript parser)
        // but Python generator should still produce integer-like output if they are whole numbers.
        val ast = ModuleNode(
            body = listOf(
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "fib", ctx = Load),
                        args = listOf(
                            ConstantNode(value = 0.0),
                            ConstantNode(value = 1.0)
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )
        val expectedCode = "fib(0, 1)" // Expecting conversion to integer string representation
        val actualCode = PythonGenerator().generateWithMetadata(ast).code // Changed: Instantiate PythonGenerator
        assertEquals(expectedCode, actualCode, "Generated Python code for fib(0, 1) with double constants did not match expected.")
    }
}

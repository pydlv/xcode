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
        val actualCode = PythonGenerator.generate(ast)
        assertEquals(expectedCode, actualCode, "Generated Python code for print with simple addition did not match expected.")
    }

    @Test
    fun `test generating simple addition expression`() {
        val ast = BinaryOpNode(
            left = ConstantNode(value = 1),
            op = "+",
            right = ConstantNode(value = 2)
        )
        // The PythonGenerator.generate() takes an AstNode, which could be a ModuleNode or an ExpressionNode directly for snippets.
        // For testing an expression directly, we might need to decide if generate() should handle raw ExpressionNodes
        // or if we should wrap it in a ModuleNode/ExprNode for the test.
        // For now, let's assume we test it as part of a statement, similar to how it would be used.
        val moduleAst = ModuleNode(body = listOf(ExprNode(value = ast)))
        val expectedCode = "1 + 2"
        // PythonGenerator.generateStatement(ExprNode(ast)) might be more direct if we want to test expression generation.
        // However, the public API is generate(AstNode), so we test through that.
        // The generate(ModuleNode) will call generateStatement, which calls generateExpression.
        // If the result is just "1 + 2", it implies the ExprNode wrapper doesn't add extra syntax like print().
        val actualCode = PythonGenerator.generate(moduleAst)
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
        val actualCode = PythonGenerator.generate(ast)
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
        val actualCode = PythonGenerator.generate(ast)
        assertEquals(expectedCode, actualCode, "Generated Python code for fib(0, 1) with double constants did not match expected.")
    }
}

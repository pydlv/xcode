package org.giraffemail.xcode.javaparser

import org.giraffemail.xcode.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaParserTest {

    @Test
    fun `test parse recursive fibonacci function`() {
        val javaCode = """public static void fib(Object a, Object b) {
        c = a + b;
        System.out.println(c);
        fib(b, c);
    }
fib(0, 1);"""

        val functionBody = listOf(
            AssignNode(
                target = NameNode(id = "c", ctx = Store),
                value = BinaryOpNode(
                    left = NameNode(id = "a", ctx = Load),
                    op = "+",
                    right = NameNode(id = "b", ctx = Load)
                )
            ),
            PrintNode(
                expression = NameNode(id = "c", ctx = Load)
            ),
            CallStatementNode(
                call = CallNode(
                    func = NameNode(id = "fib", ctx = Load),
                    args = listOf(
                        NameNode(id = "b", ctx = Load),
                        NameNode(id = "c", ctx = Load)
                    ),
                    keywords = emptyList()
                )
            )
        )

        val expectedAst = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "fib",
                    args = listOf(NameNode(id = "a", ctx = Param), NameNode(id = "b", ctx = Param)),
                    body = functionBody,
                    decorator_list = emptyList()
                ),
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "fib", ctx = Load),
                        args = listOf(
                            ConstantNode(0),
                            ConstantNode(1)
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )

        val actualAst = JavaParser.parse(javaCode)
        assertEquals(expectedAst, actualAst, "AST from Java parser is not as expected for Fibonacci function.")
    }
}

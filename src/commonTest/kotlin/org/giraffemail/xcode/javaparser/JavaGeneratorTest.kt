package org.giraffemail.xcode.javaparser

import org.giraffemail.xcode.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaGeneratorTest {

    @Test
    fun `test generating simple print statement`() {
        val ast = ModuleNode(
            body = listOf(
                PrintNode(
                    expression = ConstantNode(value = "Hello, World!")
                )
            )
        )
        val expectedCode = "System.out.println(\"Hello, World!\");"
        val actualCode = JavaGenerator().generate(ast)
        assertEquals(expectedCode, actualCode, "Generated Java code did not match expected.")
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
                        PrintNode(
                            expression = ConstantNode(value = "x is 5")
                        )
                    ),
                    orelse = emptyList()
                )
            )
        )
        val expectedCode = "if (x == 5) {\n    System.out.println(\"x is 5\");\n}"
        val actualCode = JavaGenerator().generate(ast)
        assertEquals(expectedCode, actualCode, "Generated Java if statement without else did not match expected.")
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
                        PrintNode(
                            expression = ConstantNode(value = "x is 5")
                        )
                    ),
                    orelse = listOf(
                        PrintNode(
                            expression = ConstantNode(value = "x is not 5")
                        )
                    )
                )
            )
        )
        val expectedCode = "if (x == 5) {\n    System.out.println(\"x is 5\");\n} else {\n    System.out.println(\"x is not 5\");\n}"
        val actualCode = JavaGenerator().generate(ast)
        assertEquals(expectedCode, actualCode, "Generated Java if statement with else did not match expected.")
    }

    @Test
    fun `test generating simple assignment`() {
        val ast = ModuleNode(
            body = listOf(
                AssignNode(
                    target = NameNode(id = "x", ctx = Store),
                    value = ConstantNode(value = 42)
                )
            )
        )
        val expectedCode = "x = 42;"
        val actualCode = JavaGenerator().generate(ast)
        assertEquals(expectedCode, actualCode, "Generated Java assignment did not match expected.")
    }
}
package org.giraffemail.xcode.typescriptparser

import org.giraffemail.xcode.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeScriptGeneratorTest {

    private val generator = TypeScriptGenerator()

    @Test
    fun `test generating console log with string`() {
        val ast = ModuleNode(
            body = listOf(
                PrintNode(expression = ConstantNode(value = "Hello TypeScript!"))
            )
        )

        val expectedCode = "console.log('Hello TypeScript!');"
        val generatedCode = generator.generateWithNativeMetadata(ast).code

        assertEquals(expectedCode, generatedCode, "Generated TypeScript code should match expected output")
    }

    @Test
    fun `test generating variable assignment`() {
        val ast = ModuleNode(
            body = listOf(
                AssignNode(
                    target = NameNode(id = "message", ctx = Store),
                    value = ConstantNode(value = "Hello World")
                )
            )
        )

        val expectedCode = "let message = 'Hello World';"
        val generatedCode = generator.generateWithNativeMetadata(ast).code

        assertEquals(expectedCode, generatedCode, "Generated TypeScript assignment should match expected output")
    }

    @Test
    fun `test generating function call`() {
        val ast = ModuleNode(
            body = listOf(
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "processData", ctx = Load),
                        args = listOf(
                            ConstantNode(value = 42),
                            ConstantNode(value = "test")
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )

        val expectedCode = "processData(42, 'test');"
        val generatedCode = generator.generateWithNativeMetadata(ast).code

        assertEquals(expectedCode, generatedCode, "Generated TypeScript function call should match expected output")
    }

    @Test
    fun `test generating function declaration`() {
        val ast = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "add",
                    args = listOf(
                        NameNode(id = "a", ctx = Param),
                        NameNode(id = "b", ctx = Param)
                    ),
                    body = listOf(
                        AssignNode(
                            target = NameNode(id = "result", ctx = Store),
                            value = BinaryOpNode(
                                left = NameNode(id = "a", ctx = Load),
                                op = "+",
                                right = NameNode(id = "b", ctx = Load)
                            )
                        ),
                        PrintNode(expression = NameNode(id = "result", ctx = Load))
                    ),
                    decoratorList = emptyList()
                )
            )
        )

        val expectedCode = """function add(a, b): void {
    let result = a + b;
    console.log(result);
}"""
        val generatedCode = generator.generateWithNativeMetadata(ast).code

        assertEquals(expectedCode, generatedCode, "Generated TypeScript function should match expected output")
    }

    @Test
    fun `test generating if statement`() {
        val ast = ModuleNode(
            body = listOf(
                IfNode(
                    test = CompareNode(
                        left = NameNode(id = "x", ctx = Load),
                        op = ">",
                        right = ConstantNode(value = 5)
                    ),
                    body = listOf(
                        PrintNode(expression = ConstantNode(value = "greater"))
                    ),
                    orelse = listOf(
                        PrintNode(expression = ConstantNode(value = "lesser"))
                    )
                )
            )
        )

        val expectedCode = """if (x > 5) {
    console.log('greater');
} else {
    console.log('lesser');
}"""
        val generatedCode = generator.generateWithNativeMetadata(ast).code

        assertEquals(expectedCode, generatedCode, "Generated TypeScript if statement should match expected output")
    }

    @Test
    fun `test generating binary operation`() {
        val ast = ModuleNode(
            body = listOf(
                PrintNode(
                    expression = BinaryOpNode(
                        left = ConstantNode(value = 10),
                        op = "+",
                        right = ConstantNode(value = 20)
                    )
                )
            )
        )

        val expectedCode = "console.log(10 + 20);"
        val generatedCode = generator.generateWithNativeMetadata(ast).code

        assertEquals(expectedCode, generatedCode, "Generated TypeScript binary operation should match expected output")
    }

    @Test
    fun `test generating comparison with strict equality`() {
        val ast = ModuleNode(
            body = listOf(
                PrintNode(
                    expression = CompareNode(
                        left = NameNode(id = "a", ctx = Load),
                        op = "==",
                        right = NameNode(id = "b", ctx = Load)
                    )
                )
            )
        )

        val expectedCode = "console.log(a === b);"
        val generatedCode = generator.generateWithNativeMetadata(ast).code

        assertEquals(expectedCode, generatedCode, "Generated TypeScript comparison should use strict equality")
    }
}
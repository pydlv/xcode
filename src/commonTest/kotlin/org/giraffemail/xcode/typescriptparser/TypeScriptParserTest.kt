package org.giraffemail.xcode.typescriptparser

import org.giraffemail.xcode.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class TypeScriptParserTest {

    @Test
    fun `test parsing console log with string`() {
        val tsCode = "console.log('cookies');"

        val expectedAst = ModuleNode(
            body = listOf(
                PrintNode(
                    expression = ConstantNode(value = "cookies")
                )
            )
        )

        try {
            val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList())
            assertNotNull(ast, "AST should not be null")
            assertEquals(expectedAst, ast, "AST did not match expected structure for console.log with string.")
        } catch (e: AstParseException) {
            fail("Parsing failed for console.log with string: ${e.message}", e)
        }
    }

    @Test
    fun `test parsing console log with arbitrary string`() {
        val customString = "TypeScript is awesome!"
        val tsCode = "console.log('$customString');"

        // Adjusted expected AST structure
        val expectedAst = ModuleNode(
            body = listOf(
                PrintNode( // ANTLR parser creates PrintNode directly
                    expression = ConstantNode(value = customString)
                )
            )
        )

        try {
            val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList())
            assertNotNull(ast, "AST should not be null")
            assertEquals(expectedAst, ast, "AST did not match expected structure for arbitrary string. \nActual: $ast\nExpected: $expectedAst")
        } catch (e: AstParseException) {
            fail("Parsing failed for console.log with arbitrary string: ${e.message}", e)
        }
    }

    @Test
    fun `test parsing console log with simple addition`() {
        val tsCode = "console.log(1 + 2);"

        // Expected AST structure - normalized to integers for common AST
        val expectedAst = ModuleNode(
            body = listOf(
                PrintNode( // ANTLR parser creates PrintNode directly
                    expression = BinaryOpNode( // This will require grammar change for TS
                        left = ConstantNode(value = 1, typeInfo = CanonicalTypes.Number), // Normalized to integer
                        op = "+",
                        right = ConstantNode(value = 2, typeInfo = CanonicalTypes.Number) // Normalized to integer
                    )
                )
            )
        )

        try {
            val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList())
            assertEquals(expectedAst, ast, "AST for console.log with addition did not match expected.")
        } catch (e: AstParseException) {
            fail("Parsing failed for console.log with addition: ${e.message}", e)
        }
    }

    @Test
    fun `test parsing function call with numeric arguments`() {
        val tsCode = "fib(0, 1);"
        val expectedAst = ModuleNode(
            body = listOf(
                CallStatementNode(
                    call = CallNode(
                        func = NameNode(id = "fib", ctx = Load),
                        args = listOf(
                            ConstantNode(value = 0, typeInfo = CanonicalTypes.Number), // Normalized to integer
                            ConstantNode(value = 1, typeInfo = CanonicalTypes.Number)  // Normalized to integer
                        ),
                        keywords = emptyList()
                    )
                )
            )
        )

        try {
            val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList())
            assertEquals(expectedAst, ast, "AST for fib(0, 1) did not match expected.")
        } catch (e: AstParseException) {
            fail("Parsing failed for fib(0, 1): ${e.message}", e)
        }
    }

    @Test
    fun `test parsing function declaration`() {
        val tsCode = "function greet(name: string) { console.log('Hello'); }"

        val expectedAst = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "greet",
                    args = listOf(NameNode(id = "name", ctx = Param, typeInfo = CanonicalTypes.String)),
                    body = listOf(
                        PrintNode(expression = ConstantNode(value = "Hello"))
                    ),
                    decoratorList = emptyList(),
                    returnType = CanonicalTypes.Void,
                    paramTypes = mapOf("name" to CanonicalTypes.String)
                )
            )
        )

        try {
            val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList())
            assertEquals(expectedAst, ast, "AST for function declaration did not match expected.")
        } catch (e: AstParseException) {
            fail("Parsing failed for function declaration: ${e.message}", e)
        }
    }

    @Test
    fun `test parsing variable assignment with type annotation`() {
        val tsCode = "let x: number = 42;"

        val expectedAst = ModuleNode(
            body = listOf(
                AssignNode(
                    target = NameNode(id = "x", ctx = Store),
                    value = ConstantNode(value = 42, typeInfo = CanonicalTypes.Number),
                    typeInfo = CanonicalTypes.Number
                )
            )
        )

        try {
            val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList())
            assertEquals(expectedAst, ast, "AST for variable assignment did not match expected.")
        } catch (e: AstParseException) {
            fail("Parsing failed for variable assignment: ${e.message}", e)
        }
    }
}
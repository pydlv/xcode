package org.giraffemail.xcode.haskellparser

import org.giraffemail.xcode.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class HaskellParserTest {

    @Test
    fun `test simple print statement`() {
        val haskellCode = "putStrLn \"hello\""
        
        try {
            val ast = HaskellParser.parse(haskellCode)
            assertNotNull(ast, "AST should not be null")
            println("Parsed AST: $ast")
            
            val expectedAst = ModuleNode(
                body = listOf(PrintNode(expression = ConstantNode("hello")))
            )
            
            assertEquals(expectedAst, ast, "AST did not match expected structure")
        } catch (e: Exception) {
            fail("Parsing failed: ${e.message}")
        }
    }

    @Test
    fun `test simple generation`() {
        val ast = ModuleNode(
            body = listOf(PrintNode(expression = ConstantNode("hello")))
        )
        
        val generator = HaskellGenerator()
        val result = generator.generate(ast)
        println("Generated code: $result")
        assertEquals("putStrLn \"hello\"", result)
    }
}
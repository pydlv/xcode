package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.typescriptparser.TypeScriptParser
import org.giraffemail.xcode.javaparser.JavaGenerator
import kotlin.test.Test
import kotlin.test.assertTrue

class SimpleTranspilerTest {

    @Test
    fun `test basic TypeScript to Java transpilation`() {
        val tsCode = """let name: string = "Alice";"""
        
        // Parse TypeScript to AST
        val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList())
        
        // Generate Java code
        val javaCode = JavaGenerator().generateWithMetadata(ast).code
        
        println("Generated Java code: $javaCode")
        
        // Verify basic functionality
        assertTrue(javaCode.contains("String name"), "Should generate String type")
        assertTrue(javaCode.contains("\"Alice\""), "Should preserve string value")
    }
}
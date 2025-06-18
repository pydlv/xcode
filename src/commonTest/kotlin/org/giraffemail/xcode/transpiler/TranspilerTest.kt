package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.typescriptparser.TypeScriptParser
import org.giraffemail.xcode.javaparser.JavaGenerator
import kotlin.test.Test
import kotlin.test.assertTrue

class TranspilerTest {

    @Test
    fun `test TypeScript to Java transpilation with sample program`() {
        val tsCode = """
            class Sample {
              function main() {
                let age: number = 30;
                let name: string = "Alice";
                let isActive: boolean = true;
                let hobbies: string[] = ["reading", "hiking"];
                let person: [string, number] = ["Bob", 25];
              }
            }
        """.trimIndent()

        // Parse TypeScript to AST
        val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList())
        
        // Generate Java code
        val javaCode = JavaGenerator().generateWithMetadata(ast).code
        
        println("Generated Java code:")
        println(javaCode)
        
        // Verify the generated Java code contains expected elements
        assertTrue(javaCode.contains("public class Sample"), "Should contain class declaration")
        assertTrue(javaCode.contains("public static void main(String[] args)"), "Should contain proper main method signature")
        assertTrue(javaCode.contains("int age = 30"), "Should contain int variable declaration")
        assertTrue(javaCode.contains("String name = \"Alice\""), "Should contain String variable declaration") 
        assertTrue(javaCode.contains("boolean isActive = true"), "Should contain boolean variable declaration")
        assertTrue(javaCode.contains("String[] hobbies = {\"reading\", \"hiking\"}"), "Should contain array declaration")
        assertTrue(javaCode.contains("Object[] person = {\"Bob\", 25}"), "Should contain tuple as Object array")
        
        // Verify type mappings work
        assertTrue(javaCode.contains("int"), "Should map number to int")
        assertTrue(javaCode.contains("String"), "Should map string to String")
        assertTrue(javaCode.contains("boolean"), "Should map boolean to boolean")
        assertTrue(javaCode.contains("String[]"), "Should map string[] to String[]")
    }
    
    @Test
    fun `test TypeScript array literal parsing`() {
        val tsCode = """let items: string[] = ["a", "b", "c"];"""
        
        val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList())
        val javaCode = JavaGenerator().generateWithMetadata(ast).code
        
        println("Array literal Java code:")
        println(javaCode)
        
        assertTrue(javaCode.contains("String[] items"), "Should generate proper array type")
        assertTrue(javaCode.contains("{\"a\", \"b\", \"c\"}"), "Should generate Java array literal syntax")
    }
    
    @Test
    fun `test TypeScript boolean literal parsing`() {
        val tsCode = """
            let flag1: boolean = true;
            let flag2: boolean = false;
        """.trimIndent()
        
        val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList())
        val javaCode = JavaGenerator().generateWithMetadata(ast).code
        
        println("Boolean literal Java code:")
        println(javaCode)
        
        assertTrue(javaCode.contains("boolean flag1 = true"), "Should handle true literal")
        assertTrue(javaCode.contains("boolean flag2 = false"), "Should handle false literal")
    }
    
    @Test
    fun `test TypeScript tuple type mapping`() {
        val tsCode = """let pair: [string, number] = ["hello", 42];"""
        
        val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList())
        val javaCode = JavaGenerator().generateWithMetadata(ast).code
        
        println("Tuple Java code:")
        println(javaCode)
        
        assertTrue(javaCode.contains("Object[] pair"), "Should map tuple to Object array")
        assertTrue(javaCode.contains("{\"hello\", 42}"), "Should generate proper initialization")
    }
}
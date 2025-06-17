package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.typescriptparser.TypeScriptGenerator
import org.giraffemail.xcode.typescriptparser.TypeScriptParser
import org.giraffemail.xcode.javascriptparser.JavaScriptGenerator
import org.giraffemail.xcode.javascriptparser.JavaScriptParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for metadata preservation across transpilation chains.
 * Verifies that TypeScript type information is preserved when transpiling
 * through languages that don't support types.
 */
class MetadataPreservationTest {

    @Test
    fun `test TypeScript type annotation extraction`() {
        val tsCode = """
            function greet(name: string): void {
                console.log('Hello');
            }
        """.trimIndent()

        println("Parsing TypeScript code: $tsCode")
        val ast = TypeScriptParser.parse(tsCode) as ModuleNode
        println("Generated AST: $ast")

        // Check that the function has metadata with type information
        val functionDef = ast.body[0] as FunctionDefNode
        assertEquals("greet", functionDef.name)
        
        // Verify metadata contains type information
        val metadata = functionDef.metadata
        println("Function metadata: $metadata")
        
        assertEquals("void", metadata?.get("returnType"))
        val paramTypes = metadata?.get("paramTypes") as? Map<*, *>
        assertEquals("string", paramTypes?.get("name"))
    }
    
    @Test
    fun `test TypeScript variable type annotation extraction`() {
        val tsCode = "let message: string = 'Hello';"

        println("Parsing TypeScript variable assignment: $tsCode")
        val ast = TypeScriptParser.parse(tsCode) as ModuleNode
        println("Generated AST: $ast")

        // Check that the assignment has metadata with type information
        val assignment = ast.body[0] as AssignNode
        assertEquals("message", assignment.target.id)
        
        // Verify metadata contains type information
        val metadata = assignment.metadata
        println("Assignment metadata: $metadata")
        
        assertEquals("string", metadata?.get("variableType"))
    }

    @Test
    fun `test TypeScript generation with type annotations from metadata`() {
        // Create an AST with metadata containing type information
        val paramMetadata = mapOf("type" to "string")
        val nameParam = NameNode(id = "name", ctx = Param, metadata = paramMetadata)
        
        val functionMetadata = mapOf(
            "returnType" to "void",
            "paramTypes" to mapOf("name" to "string")
        )
        
        val functionAst = FunctionDefNode(
            name = "greet",
            args = listOf(nameParam),
            body = listOf(
                PrintNode(expression = ConstantNode("Hello"))
            ),
            metadata = functionMetadata
        )
        
        val moduleAst = ModuleNode(body = listOf(functionAst))
        
        println("Generating TypeScript from AST with metadata...")
        val generator = TypeScriptGenerator()
        val generatedCode = generator.generate(moduleAst)
        println("Generated TypeScript code: $generatedCode")
        
        // Verify type annotations are included
        assertTrue(generatedCode.contains("name: string"))
        assertTrue(generatedCode.contains("): void"))
    }

    @Test
    fun `test JavaScript generation with metadata comments`() {
        // Create an AST with metadata containing type information
        val functionMetadata = mapOf(
            "returnType" to "void",
            "paramTypes" to mapOf("name" to "string")
        )
        
        val functionAst = FunctionDefNode(
            name = "greet",
            args = listOf(NameNode(id = "name", ctx = Param)),
            body = listOf(
                PrintNode(expression = ConstantNode("Hello"))
            ),
            metadata = functionMetadata
        )
        
        val moduleAst = ModuleNode(body = listOf(functionAst))
        
        println("Generating JavaScript from AST with metadata...")
        val generator = JavaScriptGenerator()
        val generatedCode = generator.generate(moduleAst)
        println("Generated JavaScript code: $generatedCode")
        
        // Verify metadata is serialized into comments
        assertTrue(generatedCode.contains("__META__"))
        assertTrue(generatedCode.contains("returnType"))
        assertTrue(generatedCode.contains("void"))
    }

    @Test
    fun `test round-trip TypeScript to JavaScript to TypeScript type preservation`() {
        // Original TypeScript code with type annotations
        val originalTsCode = """
            function greet(name: string): void {
                console.log('Hello');
            }
        """.trimIndent()
        
        println("=== TypeScript → JavaScript → TypeScript Round-Trip Test ===")
        println("Original TypeScript:")
        println(originalTsCode)
        
        // Step 1: Parse TypeScript to AST
        println("\n1. Parsing TypeScript to AST...")
        val tsAst = TypeScriptParser.parse(originalTsCode) as ModuleNode
        val functionDef = tsAst.body[0] as FunctionDefNode
        println("Extracted metadata: ${functionDef.metadata}")
        
        // Step 2: Generate JavaScript with metadata comments
        println("\n2. Generating JavaScript with metadata comments...")
        val jsGenerator = JavaScriptGenerator()
        val jsCode = jsGenerator.generate(tsAst)
        println("Generated JavaScript:")
        println(jsCode)
        
        // Verify metadata is preserved in comments
        assertTrue(jsCode.contains("__META__"))
        assertTrue(jsCode.contains("returnType"))
        assertTrue(jsCode.contains("void"))
        
        // Step 3: Parse JavaScript back to AST (this should extract metadata from comments)
        println("\n3. Parsing JavaScript back to AST...")
        val jsAst = JavaScriptParser.parse(jsCode) as ModuleNode
        val jsFunctionDef = jsAst.body[0] as FunctionDefNode
        println("JavaScript AST metadata: ${jsFunctionDef.metadata}")
        
        // Step 4: Generate TypeScript from JavaScript AST (should restore type annotations)
        println("\n4. Generating TypeScript from JavaScript AST...")
        val tsGenerator = TypeScriptGenerator()
        val finalTsCode = tsGenerator.generate(jsAst)
        println("Final TypeScript:")
        println(finalTsCode)
        
        // Verify type annotations are restored
        assertTrue(finalTsCode.contains("name: string"))
        assertTrue(finalTsCode.contains("): void"))
        
        println("\n=== Round-trip test completed successfully! ===")
    }

    @Test
    fun `test MetadataSerializer serialization and deserialization`() {
        val metadata = LanguageMetadata(
            returnType = "void",
            paramTypes = mapOf("name" to "string", "age" to "number")
        )
        
        println("Original metadata: $metadata")
        
        val serialized = MetadataSerializer.serialize(metadata)
        println("Serialized JSON: $serialized")
        
        val deserialized = MetadataSerializer.deserialize(serialized)
        println("Deserialized metadata: $deserialized")
        
        assertEquals("void", deserialized?.returnType)
        assertEquals("string", deserialized?.paramTypes?.get("name"))
        assertEquals("number", deserialized?.paramTypes?.get("age"))
    }
}
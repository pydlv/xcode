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
        val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList()) as ModuleNode
        println("Generated AST: $ast")

        // Check that the function has metadata with type information
        val functionDef = ast.body[0] as FunctionDefNode
        assertEquals("greet", functionDef.name)
        
        // Verify explicit fields contain type information
        println("Function returnType: ${functionDef.returnType}")
        println("Function paramTypes: ${functionDef.paramTypes}")
        
        assertEquals(CanonicalTypes.Void, functionDef.returnType)
        assertEquals(CanonicalTypes.String, functionDef.paramTypes["name"])
    }
    
    @Test
    fun `test TypeScript variable type annotation extraction`() {
        val tsCode = "let message: string = 'Hello';"

        println("Parsing TypeScript variable assignment: $tsCode")
        val ast = TypeScriptParser.parseWithMetadata(tsCode, emptyList()) as ModuleNode
        println("Generated AST: $ast")

        // Check that the assignment has type information
        val assignment = ast.body[0] as AssignNode
        assertEquals("message", assignment.target.id)
        
        // Verify explicit field contains type information
        println("Assignment variableType: ${assignment.variableType}")
        
        assertEquals(CanonicalTypes.String, assignment.variableType)
    }

    @Test
    fun `test TypeScript generation with type annotations from metadata`() {
        // Create an AST with explicit type information
        val nameParam = NameNode(id = "name", ctx = Param, type = CanonicalTypes.String)
        
        val functionAst = FunctionDefNode(
            name = "greet",
            args = listOf(nameParam),
            body = listOf(
                PrintNode(expression = ConstantNode("Hello"))
            ),
            returnType = CanonicalTypes.Void,
            paramTypes = mapOf("name" to CanonicalTypes.String)
        )
        
        val moduleAst = ModuleNode(body = listOf(functionAst))
        
        println("Generating TypeScript from AST with metadata...")
        val generator = TypeScriptGenerator()
        val generatedCode = generator.generateWithMetadata(moduleAst).code
        println("Generated TypeScript code: $generatedCode")
        
        // Verify type annotations are included
        assertTrue(generatedCode.contains("name: string"))
        assertTrue(generatedCode.contains("): void"))
    }

    @Test
    fun `test JavaScript generation with parts-based metadata`() {
        // Create an AST with explicit type information
        val functionAst = FunctionDefNode(
            name = "greet",
            args = listOf(NameNode(id = "name", ctx = Param)),
            body = listOf(
                PrintNode(expression = ConstantNode("Hello"))
            ),
            returnType = CanonicalTypes.Void,
            paramTypes = mapOf("name" to CanonicalTypes.String)
        )
        
        val moduleAst = ModuleNode(body = listOf(functionAst))
        
        println("Generating JavaScript from AST with metadata...")
        val generator = JavaScriptGenerator()
        val codeWithMetadata = generator.generateWithMetadata(moduleAst)
        println("Generated JavaScript code: ${codeWithMetadata.code}")
        println("Generated metadata part: ${codeWithMetadata.metadata}")
        
        // Verify metadata is NOT in comments but in separate part
        assertTrue(!codeWithMetadata.code.contains("__META__"))
        assertTrue(codeWithMetadata.code.contains("function greet(name)"))
        assertTrue(codeWithMetadata.code.contains("console.log('Hello')"))
        
        // Verify metadata is in the separate part
        assertTrue(codeWithMetadata.metadata.isNotEmpty())
        val functionMetadataItem = codeWithMetadata.metadata.first()
        assertEquals("void", functionMetadataItem.returnType)
        assertEquals("string", functionMetadataItem.paramTypes["name"])
    }

    @Test
    fun `test round-trip TypeScript to JavaScript to TypeScript type preservation`() {
        // Original TypeScript code with type annotations
        val originalTsCode = """
            function greet(name: string): void {
                console.log('Hello');
            }
        """.trimIndent()
        
        println("=== TypeScript → JavaScript → TypeScript Parts-Based Round-Trip Test ===")
        println("Original TypeScript:")
        println(originalTsCode)
        
        // Step 1: Parse TypeScript to AST
        println("\n1. Parsing TypeScript to AST...")
        val tsAst = TypeScriptParser.parseWithMetadata(originalTsCode, emptyList()) as ModuleNode
        val functionDef = tsAst.body[0] as FunctionDefNode
        println("Extracted returnType: ${functionDef.returnType}")
        println("Extracted paramTypes: ${functionDef.paramTypes}")
        
        // Step 2: Generate JavaScript with metadata parts separately
        println("\n2. Generating JavaScript with parts-based metadata...")
        val jsGenerator = JavaScriptGenerator()
        val jsCodeWithMetadata = jsGenerator.generateWithMetadata(tsAst)
        println("Generated JavaScript:")
        println(jsCodeWithMetadata.code)
        println("Generated metadata part:")
        println(jsCodeWithMetadata.metadata)
        
        // Verify metadata is NOT in comments but in separate part
        assertTrue(!jsCodeWithMetadata.code.contains("__META__"))
        assertTrue(jsCodeWithMetadata.code.contains("function greet(name)"))
        assertTrue(jsCodeWithMetadata.metadata.isNotEmpty())
        
        // Step 3: Parse JavaScript back to AST using parts-based metadata
        println("\n3. Parsing JavaScript back to AST with metadata part...")
        val jsAst = JavaScriptParser.parseWithMetadata(jsCodeWithMetadata.code, jsCodeWithMetadata.metadata) as ModuleNode
        val jsFunctionDef = jsAst.body[0] as FunctionDefNode
        println("JavaScript AST returnType: ${jsFunctionDef.returnType}")
        println("JavaScript AST paramTypes: ${jsFunctionDef.paramTypes}")
        
        // Step 4: Generate TypeScript from JavaScript AST (should restore type annotations)
        println("\n4. Generating TypeScript from JavaScript AST...")
        val tsGenerator = TypeScriptGenerator()
        val finalTsCode = tsGenerator.generateWithMetadata(jsAst).code
        println("Final TypeScript:")
        println(finalTsCode)
        
        // Verify type annotations are restored
        assertTrue(finalTsCode.contains("name: string"))
        assertTrue(finalTsCode.contains("): void"))
        
        println("\n=== Parts-based round-trip test completed successfully! ===")
    }

    @Test
    fun `test parts-based metadata object handling`() {
        val metadata = LanguageMetadata(
            returnType = "void",
            paramTypes = mapOf("name" to "string", "age" to "number")
        )
        
        println("Original metadata: $metadata")
        
        // Create CodeWithMetadata using object-based approach
        val codeWithMetadata = MetadataSerializer.createCodeWithMetadata("test code", listOf(metadata))
        println("Code with metadata: $codeWithMetadata")
        
        // Verify metadata is stored as objects, not strings
        val extractedMetadata = codeWithMetadata.metadata.first()
        assertEquals("void", extractedMetadata.returnType)
        assertEquals("string", extractedMetadata.paramTypes["name"])
        assertEquals("number", extractedMetadata.paramTypes["age"])
        
        println("✓ Parts-based metadata handling verified")
    }
}
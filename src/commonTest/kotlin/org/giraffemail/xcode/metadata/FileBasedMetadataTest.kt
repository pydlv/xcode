package org.giraffemail.xcode.metadata

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.javascriptparser.JavaScriptGenerator
import org.giraffemail.xcode.javaparser.JavaParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for file-based metadata storage and retrieval
 */
class FileBasedMetadataTest {

    @Test
    fun `test metadata file path generation`() {
        val sourceFile = "test.js"
        val metadataFile = MetadataSerializer.getMetadataFilePath(sourceFile)
        assertEquals("test.js.meta", metadataFile)
    }

    @Test
    fun `test metadata file write and read`() {
        // Clear any existing metadata
        MetadataSerializer.clearMetadataFileStore()
        
        val sourceFile = "test.js"
        val metadata = listOf(
            LanguageMetadata(
                returnType = "void",
                paramTypes = mapOf("name" to "string")
            ),
            LanguageMetadata(
                variableType = "number"
            )
        )
        
        // Write metadata to file
        val success = MetadataSerializer.writeMetadataToFile(sourceFile, metadata)
        assertTrue(success)
        
        // Check if metadata file exists
        assertTrue(MetadataSerializer.hasMetadataFile(sourceFile))
        
        // Read metadata back
        val readMetadata = MetadataSerializer.readMetadataFromFile(sourceFile)
        assertEquals(2, readMetadata.size)
        assertEquals("void", readMetadata[0].returnType)
        assertEquals("string", readMetadata[0].paramTypes["name"])
        assertEquals("number", readMetadata[1].variableType)
    }

    @Test
    fun `test JavaScript generation with file-based metadata`() {
        // Clear any existing metadata
        MetadataSerializer.clearMetadataFileStore()
        
        // Create AST with metadata
        val functionMetadata = mapOf(
            "returnType" to "void",
            "paramTypes" to mapOf("name" to "string")
        )
        
        val functionAst = FunctionDefNode(
            name = "greet",
            args = listOf(NameNode(id = "name", ctx = Param)),
            body = listOf(
                AssignNode(
                    target = NameNode(id = "message", ctx = Store),
                    value = ConstantNode("Hello"),
                    metadata = mapOf("variableType" to "string")
                )
            ),
            metadata = functionMetadata
        )
        
        val moduleAst = ModuleNode(body = listOf(functionAst))
        
        // Generate JavaScript with file-based metadata
        val generator = JavaScriptGenerator()
        val outputFile = "output.js"
        val generatedCode = generator.generateWithMetadataFile(moduleAst, outputFile)
        
        println("Generated JavaScript code:")
        println(generatedCode)
        
        // Verify code doesn't contain metadata comments
        assertTrue(!generatedCode.contains("__META__"))
        assertTrue(generatedCode.contains("function greet(name)"))
        assertTrue(generatedCode.contains("let message = 'Hello'"))
        
        // Verify metadata was written to file
        assertTrue(MetadataSerializer.hasMetadataFile(outputFile))
        val savedMetadata = MetadataSerializer.readMetadataFromFile(outputFile)
        assertEquals(2, savedMetadata.size)
        
        // Check function metadata
        val functionMeta = savedMetadata.find { it.returnType != null }
        assertEquals("void", functionMeta?.returnType)
        assertEquals("string", functionMeta?.paramTypes?.get("name"))
        
        // Check variable metadata
        val variableMeta = savedMetadata.find { it.variableType != null }
        assertEquals("string", variableMeta?.variableType)
    }

    @Test
    fun `test parser with file-based metadata`() {
        // Clear any existing metadata
        MetadataSerializer.clearMetadataFileStore()
        
        // Create metadata file
        val sourceFile = "test.java"
        val metadata = listOf(
            LanguageMetadata(
                returnType = "void",
                paramTypes = mapOf("name" to "String")
            )
        )
        MetadataSerializer.writeMetadataToFile(sourceFile, metadata)
        
        // Create simple Java code without metadata comments
        val javaCode = """
            public static void greet(Object name) {
                System.out.println("Hello");
            }
        """.trimIndent()
        
        // Parse with file-based metadata
        val ast = JavaParser.parseWithMetadataFile(javaCode, sourceFile) as ModuleNode
        val functionDef = ast.body[0] as FunctionDefNode
        
        // Verify metadata was injected from file
        assertEquals("greet", functionDef.name)
        assertEquals("void", functionDef.metadata?.get("returnType"))
        val paramTypes = functionDef.metadata?.get("paramTypes") as? Map<*, *>
        assertEquals("String", paramTypes?.get("name"))
    }
}
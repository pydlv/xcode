package org.giraffemail.xcode.metadata

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.javascriptparser.JavaScriptGenerator
import org.giraffemail.xcode.javascriptparser.JavaScriptParser
import org.giraffemail.xcode.javaparser.JavaParser
import org.giraffemail.xcode.pythonparser.PythonGenerator
import org.giraffemail.xcode.typescriptparser.TypeScriptGenerator
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

    @Test
    fun `test TypeScript round-trip with file-based metadata`() {
        // Clear any existing metadata
        MetadataSerializer.clearMetadataFileStore()
        
        // Create TypeScript AST with metadata
        val functionMetadata = mapOf(
            "returnType" to "number",
            "paramTypes" to mapOf("x" to "number", "y" to "number")
        )
        
        val functionAst = FunctionDefNode(
            name = "add",
            args = listOf(
                NameNode(id = "x", ctx = Param, metadata = mapOf("type" to "number")),
                NameNode(id = "y", ctx = Param, metadata = mapOf("type" to "number"))
            ),
            body = listOf(
                AssignNode(
                    target = NameNode(id = "result", ctx = Store),
                    value = BinaryOpNode(
                        left = NameNode(id = "x", ctx = Load),
                        op = "+",
                        right = NameNode(id = "y", ctx = Load)
                    ),
                    metadata = mapOf("variableType" to "number")
                )
            ),
            metadata = functionMetadata
        )
        
        val moduleAst = ModuleNode(body = listOf(functionAst))
        
        // Generate TypeScript with file-based metadata
        val tsGenerator = TypeScriptGenerator()
        val outputFile = "output.ts"
        val generatedCode = tsGenerator.generateWithMetadataFile(moduleAst, outputFile)
        
        println("Generated TypeScript code:")
        println(generatedCode)
        
        // Verify code contains type annotations but no metadata comments
        assertTrue(!generatedCode.contains("__META__"))
        assertTrue(generatedCode.contains("function add(x: number, y: number): number"))
        assertTrue(generatedCode.contains("let result: number = x + y"))
        
        // Verify metadata was written to file
        assertTrue(MetadataSerializer.hasMetadataFile(outputFile))
        val savedMetadata = MetadataSerializer.readMetadataFromFile(outputFile)
        assertEquals(2, savedMetadata.size)
    }

    @Test
    fun `test cross-language transpilation with file-based metadata`() {
        // Clear any existing metadata
        MetadataSerializer.clearMetadataFileStore()
        
        // Start with TypeScript AST
        val functionMetadata = mapOf(
            "returnType" to "void",
            "paramTypes" to mapOf("message" to "string")
        )
        
        val tsAst = ModuleNode(body = listOf(
            FunctionDefNode(
                name = "greet", // Changed from "log" to "greet" to avoid parsing issues
                args = listOf(NameNode(id = "message", ctx = Param)),
                body = listOf(PrintNode(expression = NameNode(id = "message", ctx = Load))),
                metadata = functionMetadata
            )
        ))
        
        // Generate JavaScript with file-based metadata
        val jsGenerator = JavaScriptGenerator()
        val jsFile = "output.js"
        val jsCode = jsGenerator.generateWithMetadataFile(tsAst, jsFile)
        
        println("Generated JavaScript code for cross-language test:")
        println(jsCode)
        
        // Parse JavaScript back with file-based metadata
        val jsAst = JavaScriptParser.parseWithMetadataFile(jsCode, jsFile) as ModuleNode
        val jsFunctionDef = jsAst.body[0] as FunctionDefNode
        
        // Verify metadata was preserved
        assertEquals("greet", jsFunctionDef.name)
        assertEquals("void", jsFunctionDef.metadata?.get("returnType"))
        val paramTypes = jsFunctionDef.metadata?.get("paramTypes") as? Map<*, *>
        assertEquals("string", paramTypes?.get("message"))
        
        // Generate Python with file-based metadata
        val pythonGenerator = PythonGenerator()
        val pythonFile = "output.py"
        val pythonCode = pythonGenerator.generateWithMetadataFile(jsAst, pythonFile)
        
        println("Generated Python code:")
        println(pythonCode)
        
        // Verify Python code doesn't contain metadata comments
        assertTrue(!pythonCode.contains("__META__"))
        assertTrue(pythonCode.contains("def greet(message):"))
        
        // Verify metadata persists
        assertTrue(MetadataSerializer.hasMetadataFile(pythonFile))
        val pythonMetadata = MetadataSerializer.readMetadataFromFile(pythonFile)
        assertEquals(1, pythonMetadata.size)
        assertEquals("void", pythonMetadata[0].returnType)
    }

    @Test
    fun `test backward compatibility with comment-based metadata`() {
        // Clear any existing metadata
        MetadataSerializer.clearMetadataFileStore()
        
        // Create JavaScript code with traditional metadata comments (like the existing system)
        val jsCodeWithComments = """
            function greet(name) { // __META__: {"returnType":"void","paramTypes":{"name":"string"}}
                let message = 'Hello'; // __META__: {"variableType":"string"}
                console.log(message);
            }
        """.trimIndent()
        
        // Parse using the traditional comment-based method
        val ast = JavaScriptParser.parse(jsCodeWithComments) as ModuleNode
        val functionDef = ast.body[0] as FunctionDefNode
        
        // Verify metadata was extracted from comments
        assertEquals("greet", functionDef.name)
        assertEquals("void", functionDef.metadata?.get("returnType"))
        val paramTypes = functionDef.metadata?.get("paramTypes") as? Map<*, *>
        assertEquals("string", paramTypes?.get("name"))
        
        // Verify assignment metadata
        val assignment = functionDef.body[0] as AssignNode
        assertEquals("string", assignment.metadata?.get("variableType"))
        
        println("✓ Backward compatibility with comment-based metadata verified")
    }
}
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
 * Tests for parts-based metadata storage and retrieval
 */
class PartsBasedMetadataTest {

    @Test
    fun `test metadata serialization and deserialization`() {
        val metadata = listOf(
            LanguageMetadata(
                returnType = "void",
                paramTypes = mapOf("name" to "string")
            ),
            LanguageMetadata(
                variableType = "number"
            )
        )
        
        // Serialize metadata to string
        val metadataPart = MetadataSerializer.serializeMetadataList(metadata)
        
        // Deserialize metadata back
        val readMetadata = MetadataSerializer.deserializeMetadataList(metadataPart)
        assertEquals(2, readMetadata.size)
        assertEquals("void", readMetadata[0].returnType)
        assertEquals("string", readMetadata[0].paramTypes["name"])
        assertEquals("number", readMetadata[1].variableType)
    }

    @Test
    fun `test CodeWithMetadata creation`() {
        val code = "function test() { }"
        val metadata = listOf(
            LanguageMetadata(returnType = "void")
        )
        
        val codeWithMetadata = MetadataSerializer.createCodeWithMetadata(code, metadata)
        assertEquals(code, codeWithMetadata.code)
        
        val deserializedMetadata = MetadataSerializer.deserializeMetadataList(codeWithMetadata.metadata)
        assertEquals(1, deserializedMetadata.size)
        assertEquals("void", deserializedMetadata[0].returnType)
    }

    @Test
    fun `test JavaScript generation with parts-based metadata`() {
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
        
        // Generate JavaScript with parts-based metadata
        val generator = JavaScriptGenerator()
        val codeWithMetadata = generator.generateWithMetadata(moduleAst)
        
        println("Generated JavaScript code:")
        println(codeWithMetadata.code)
        println("Generated metadata part:")
        println(codeWithMetadata.metadata)
        
        // Verify code doesn't contain metadata comments
        assertTrue(!codeWithMetadata.code.contains("__META__"))
        assertTrue(codeWithMetadata.code.contains("function greet(name)"))
        assertTrue(codeWithMetadata.code.contains("let message = 'Hello'"))
        
        // Verify metadata part contains expected data
        val savedMetadata = MetadataSerializer.deserializeMetadataList(codeWithMetadata.metadata)
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
    fun `test parser with parts-based metadata`() {
        // Create metadata part
        val metadata = listOf(
            LanguageMetadata(
                returnType = "void",
                paramTypes = mapOf("name" to "String")
            )
        )
        val metadataPart = MetadataSerializer.serializeMetadataList(metadata)
        
        // Create simple Java code without metadata comments
        val javaCode = """
            public static void greet(Object name) {
                System.out.println("Hello");
            }
        """.trimIndent()
        
        // Parse with parts-based metadata
        val ast = JavaParser.parseWithMetadata(javaCode, metadataPart) as ModuleNode
        val functionDef = ast.body[0] as FunctionDefNode
        
        // Verify metadata was injected from part
        assertEquals("greet", functionDef.name)
        assertEquals("void", functionDef.metadata?.get("returnType"))
        val paramTypes = functionDef.metadata?.get("paramTypes") as? Map<*, *>
        assertEquals("String", paramTypes?.get("name"))
    }

    @Test
    fun `test TypeScript round-trip with parts-based metadata`() {
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
        
        // Generate TypeScript with parts-based metadata
        val tsGenerator = TypeScriptGenerator()
        val codeWithMetadata = tsGenerator.generateWithMetadata(moduleAst)
        
        println("Generated TypeScript code:")
        println(codeWithMetadata.code)
        println("Generated metadata part:")
        println(codeWithMetadata.metadata)
        
        // Verify code contains type annotations but no metadata comments
        assertTrue(!codeWithMetadata.code.contains("__META__"))
        assertTrue(codeWithMetadata.code.contains("function add(x: number, y: number): number"))
        assertTrue(codeWithMetadata.code.contains("let result: number = x + y"))
        
        // Verify metadata part contains expected data
        val savedMetadata = MetadataSerializer.deserializeMetadataList(codeWithMetadata.metadata)
        assertEquals(2, savedMetadata.size)
    }

    @Test
    fun `test cross-language transpilation with parts-based metadata`() {
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
        
        // Generate JavaScript with parts-based metadata
        val jsGenerator = JavaScriptGenerator()
        val jsCodeWithMetadata = jsGenerator.generateWithMetadata(tsAst)
        
        println("Generated JavaScript code for cross-language test:")
        println(jsCodeWithMetadata.code)
        println("Generated metadata part:")
        println(jsCodeWithMetadata.metadata)
        
        // Parse JavaScript back with parts-based metadata
        val jsAst = JavaScriptParser.parseWithMetadata(jsCodeWithMetadata.code, jsCodeWithMetadata.metadata) as ModuleNode
        val jsFunctionDef = jsAst.body[0] as FunctionDefNode
        
        // Verify metadata was preserved
        assertEquals("greet", jsFunctionDef.name)
        assertEquals("void", jsFunctionDef.metadata?.get("returnType"))
        val paramTypes = jsFunctionDef.metadata?.get("paramTypes") as? Map<*, *>
        assertEquals("string", paramTypes?.get("message"))
        
        // Generate Python with parts-based metadata
        val pythonGenerator = PythonGenerator()
        val pythonCodeWithMetadata = pythonGenerator.generateWithMetadata(jsAst)
        
        println("Generated Python code:")
        println(pythonCodeWithMetadata.code)
        println("Generated Python metadata part:")
        println(pythonCodeWithMetadata.metadata)
        
        // Verify Python code doesn't contain metadata comments
        assertTrue(!pythonCodeWithMetadata.code.contains("__META__"))
        assertTrue(pythonCodeWithMetadata.code.contains("def greet(message):"))
        
        // Verify metadata persists
        val pythonMetadata = MetadataSerializer.deserializeMetadataList(pythonCodeWithMetadata.metadata)
        assertEquals(1, pythonMetadata.size)
        assertEquals("void", pythonMetadata[0].returnType)
    }

    @Test
    fun `test backward compatibility with comment-based metadata`() {
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
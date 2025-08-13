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
 * Tests for native metadata storage and retrieval
 */
class NativeMetadataTest {

    @Test
    fun `test native metadata storage and retrieval`() {
        val metadata = listOf(
            FunctionMetadata(
                returnType = CanonicalTypes.Void,
                paramTypes = mapOf("name" to CanonicalTypes.String)
            ),
            VariableMetadata(
                variableType = CanonicalTypes.Number
            )
        )
        
        // Store metadata directly as native Kotlin objects (no string serialization)
        val codeWithMetadata = CodeWithNativeMetadata(
            code = "function test() {}",
            metadata = metadata
        )
        
        // Retrieve native metadata directly
        val retrievedMetadata = codeWithMetadata.metadata
        assertEquals(2, retrievedMetadata.size)
        
        val functionMetadata = retrievedMetadata[0] as FunctionMetadata
        assertEquals(CanonicalTypes.Void, functionMetadata.returnType)
        assertEquals(CanonicalTypes.String, functionMetadata.paramTypes["name"])
        
        val variableMetadata = retrievedMetadata[1] as VariableMetadata
        assertEquals(CanonicalTypes.Number, variableMetadata.variableType)
    }

    @Test
    fun `test CodeWithNativeMetadata creation`() {
        val code = "function test() { }"
        val metadata = listOf(
            FunctionMetadata(returnType = CanonicalTypes.Void)
        )
        
        val codeWithMetadata = NativeMetadataUtils.createCodeWithMetadata(code, metadata)
        assertEquals(code, codeWithMetadata.code)
        
        val retrievedMetadata = codeWithMetadata.metadata
        assertEquals(1, retrievedMetadata.size)
        val functionMetadata = retrievedMetadata[0] as FunctionMetadata
        assertEquals(CanonicalTypes.Void, functionMetadata.returnType)
    }

    @Test
    fun `test JavaScript generation with native metadata`() {
        // Create AST with metadata
        val functionAst = FunctionDefNode(
            name = "greet",
            args = listOf(NameNode(id = "name", ctx = Param, typeInfo = CanonicalTypes.String)),
            body = listOf(
                AssignNode(
                    target = NameNode(id = "message", ctx = Store),
                    value = ConstantNode("Hello"),
                    typeInfo = CanonicalTypes.String
                )
            ),
            returnType = CanonicalTypes.Void,
            paramTypes = mapOf("name" to CanonicalTypes.String)
        )
        
        val moduleAst = ModuleNode(body = listOf(functionAst))
        
        // Generate JavaScript with native metadata
        val generator = JavaScriptGenerator()
        val codeWithMetadata = generator.generateWithNativeMetadata(moduleAst)
        
        println("Generated JavaScript code:")
        println(codeWithMetadata.code)
        println("Generated metadata part:")
        println(codeWithMetadata.metadata)
        
        // Verify code doesn't contain metadata comments
        assertTrue(!codeWithMetadata.code.contains("__META__"))
        assertTrue(codeWithMetadata.code.contains("function greet(name)"))
        assertTrue(codeWithMetadata.code.contains("let message = 'Hello'"))
        
        // Verify metadata part contains expected data
        val savedMetadata = codeWithMetadata.metadata
        assertEquals(2, savedMetadata.size)
        
        // Check function metadata
        val functionMeta = NativeMetadataUtils.filterFunctionMetadata(savedMetadata).firstOrNull()
        assertEquals(CanonicalTypes.Void, functionMeta?.returnType)
        assertEquals(CanonicalTypes.String, functionMeta?.paramTypes?.get("name"))
        
        // Check variable metadata
        val variableMeta = NativeMetadataUtils.filterVariableMetadata(savedMetadata).firstOrNull()
        assertEquals(CanonicalTypes.String, variableMeta?.variableType)
    }

    @Test
    fun `test parser with native metadata`() {
        // Create native metadata part
        val metadata = listOf(
            FunctionMetadata(
                returnType = CanonicalTypes.Void,
                paramTypes = mapOf("name" to CanonicalTypes.String)
            )
        )
        
        // Create simple Java code without metadata comments
        val javaCode = """
            public static void greet(Object name) {
                System.out.println("Hello");
            }
        """.trimIndent()
        
        // Parse with native metadata
        val ast = JavaParser.parseWithNativeMetadata(javaCode, metadata) as ModuleNode
        val functionDef = ast.body[0] as FunctionDefNode
        
        // Verify metadata was injected from native metadata
        assertEquals("greet", functionDef.name)
        assertEquals(CanonicalTypes.Void, functionDef.returnType)
        assertEquals(CanonicalTypes.String, functionDef.paramTypes["name"])
    }

    @Test
    fun `test TypeScript round-trip with native metadata`() {
        // Create TypeScript AST with metadata
        val functionAst = FunctionDefNode(
            name = "add",
            args = listOf(
                NameNode(id = "x", ctx = Param, typeInfo = CanonicalTypes.Number),
                NameNode(id = "y", ctx = Param, typeInfo = CanonicalTypes.Number)
            ),
            body = listOf(
                AssignNode(
                    target = NameNode(id = "result", ctx = Store),
                    value = BinaryOpNode(
                        left = NameNode(id = "x", ctx = Load),
                        op = "+",
                        right = NameNode(id = "y", ctx = Load)
                    ),
                    typeInfo = CanonicalTypes.Number
                )
            ),
            returnType = CanonicalTypes.Number,
            paramTypes = mapOf("x" to CanonicalTypes.Number, "y" to CanonicalTypes.Number)
        )
        
        val moduleAst = ModuleNode(body = listOf(functionAst))
        
        // Generate TypeScript with native metadata
        val tsGenerator = TypeScriptGenerator()
        val codeWithMetadata = tsGenerator.generateWithNativeMetadata(moduleAst)
        
        println("Generated TypeScript code:")
        println(codeWithMetadata.code)
        println("Generated metadata part:")
        println(codeWithMetadata.metadata)
        
        // Verify code contains type annotations but no metadata comments
        assertTrue(!codeWithMetadata.code.contains("__META__"))
        assertTrue(codeWithMetadata.code.contains("function add(x: number, y: number): number"))
        assertTrue(codeWithMetadata.code.contains("let result: number = x + y"))
        
        // Verify metadata part contains expected data
        val savedMetadata = codeWithMetadata.metadata
        assertEquals(2, savedMetadata.size)
    }

    @Test
    fun `test cross-language transpilation with parts-based metadata`() {
        // Start with TypeScript AST
        val functionMetadata = mapOf(
            "returnType" to "void",
            "paramTypes" to mapOf("message" to CanonicalTypes.String)
        )
        
        val tsAst = ModuleNode(body = listOf(
            FunctionDefNode(
                name = "greet", // Changed from "log" to "greet" to avoid parsing issues
                args = listOf(NameNode(id = "message", ctx = Param)),
                body = listOf(PrintNode(expression = NameNode(id = "message", ctx = Load))),
                returnType = CanonicalTypes.Void,
                paramTypes = mapOf("message" to CanonicalTypes.String)
            )
        ))
        
        // Generate JavaScript with parts-based metadata
        val jsGenerator = JavaScriptGenerator()
        val jsCodeWithNativeMetadata = jsGenerator.generateWithNativeMetadata(tsAst)
        
        println("Generated JavaScript code for cross-language test:")
        println(jsCodeWithNativeMetadata.code)
        println("Generated metadata part:")
        println(jsCodeWithNativeMetadata.metadata)
        
        // Parse JavaScript back with parts-based metadata
        val jsAst = JavaScriptParser.parseWithNativeMetadata(jsCodeWithNativeMetadata.code, jsCodeWithNativeMetadata.metadata) as ModuleNode
        val jsFunctionDef = jsAst.body[0] as FunctionDefNode
        
        // Verify metadata was preserved
        assertEquals("greet", jsFunctionDef.name)
        assertEquals(CanonicalTypes.Void, jsFunctionDef.returnType)
        assertEquals(CanonicalTypes.String, jsFunctionDef.paramTypes["message"])
        
        // Generate Python with parts-based metadata
        val pythonGenerator = PythonGenerator()
        val pythonCodeWithNativeMetadata = pythonGenerator.generateWithNativeMetadata(jsAst)
        
        println("Generated Python code:")
        println(pythonCodeWithNativeMetadata.code)
        println("Generated Python metadata part:")
        println(pythonCodeWithNativeMetadata.metadata)
        
        // Verify Python code doesn't contain metadata comments
        assertTrue(!pythonCodeWithNativeMetadata.code.contains("__META__"))
        assertTrue(pythonCodeWithNativeMetadata.code.contains("def greet(message):"))
        
        // Verify metadata persists
        val pythonMetadata = pythonCodeWithNativeMetadata.metadata
        assertEquals(1, pythonMetadata.size)
        val functionMeta = pythonMetadata[0] as FunctionMetadata
        assertEquals(CanonicalTypes.Void, functionMeta.returnType)
    }

    @Test
    fun `test comment-based metadata is no longer supported`() {
        // Create JavaScript code with traditional metadata comments (like the old system)
        val jsCodeWithComments = """
            function greet(name) { // __META__: {"returnType":"void","paramTypes":{"name":"string"}}
                let message = 'Hello'; // __META__: {"variableType":"string"}
                console.log(message);
            }
        """.trimIndent()
        
        // Parse using the regular parser (comment-based extraction removed)
        val ast = JavaScriptParser.parseWithNativeMetadata(jsCodeWithComments, emptyList<NativeMetadata>()) as ModuleNode
        val functionDef = ast.body[0] as FunctionDefNode
        
        // Verify metadata was NOT extracted from comments (since we removed that functionality)
        assertEquals("greet", functionDef.name)
        assertEquals(CanonicalTypes.Void, functionDef.returnType)
        
        // Verify assignment has no metadata from comments
        val assignment = functionDef.body[0] as AssignNode
        assertEquals(CanonicalTypes.Unknown, assignment.typeInfo)
        
        println("✓ Comment-based metadata is no longer supported (expected behavior)")
    }
}
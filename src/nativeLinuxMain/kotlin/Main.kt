package org.giraffemail.xcode

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.typescriptparser.TypeScriptGenerator
import org.giraffemail.xcode.typescriptparser.TypeScriptParser
import org.giraffemail.xcode.javascriptparser.JavaScriptGenerator
import org.giraffemail.xcode.javascriptparser.JavaScriptParser

fun debugMetadataHandling() {
    println("=== Debug Metadata Handling ===")
    
    // Test 1: Simple TypeScript with type annotations
    val tsCodeWithTypes = """
        function greet(name: string): void {
            console.log('Hello');
        }
    """.trimIndent()
    
    println("1. Parsing TypeScript with types:")
    println(tsCodeWithTypes)
    val tsAst = TypeScriptParser.parse(tsCodeWithTypes) as ModuleNode
    val functionDef = tsAst.body[0] as FunctionDefNode
    println("   Function metadata: ${functionDef.metadata}")
    
    // Test 2: Generate JavaScript with metadata comments
    println("\n2. Generating JavaScript with metadata comments:")
    val jsGenerator = JavaScriptGenerator()
    val jsCode = jsGenerator.generate(tsAst)
    println(jsCode)
    
    // Test 3: Parse JavaScript back to see if metadata is extracted
    println("\n3. Parsing JavaScript back to AST:")
    val jsAst = JavaScriptParser.parse(jsCode) as ModuleNode
    val jsFunctionDef = jsAst.body[0] as FunctionDefNode
    println("   JavaScript AST metadata: ${jsFunctionDef.metadata}")
    
    // Test 4: Generate TypeScript to see if types are restored
    println("\n4. Generating TypeScript from JavaScript AST:")
    val tsGenerator = TypeScriptGenerator()
    val finalTs = tsGenerator.generate(jsAst)
    println(finalTs)
}

fun main() {
    debugMetadataHandling()
}

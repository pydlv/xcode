package org.giraffemail.xcode.execution

import org.giraffemail.xcode.javaparser.JavaGenerator
import org.giraffemail.xcode.javaparser.JavaParser
import org.giraffemail.xcode.pythonparser.PythonGenerator
import org.giraffemail.xcode.pythonparser.PythonParser
import org.giraffemail.xcode.typescriptparser.TypeScriptGenerator
import org.giraffemail.xcode.typescriptparser.TypeScriptParser
import org.giraffemail.xcode.cli.FileOperations
import kotlin.test.Test

/**
 * Simple test to inspect the transpiled Java code and identify issues.
 */
class TranspilationInspectionTest {

    @Test
    fun `inspect transpiled Java code structure`() {
        val originalJavaPath = "samples/java/BasicJavaDemo.java"
        
        println("=== Inspecting Round-Trip Transpilation ===")
        
        try {
            // Step 1: Read original Java file
            val originalJavaCode = FileOperations.readFileContent(originalJavaPath)
            println("Original Java code:")
            println(originalJavaCode)
            println("\n" + "=".repeat(80) + "\n")
            
            // Step 2: Java → Python
            println("--- Step 1: Java → Python ---")
            val pythonAst = JavaParser.parseWithNativeMetadata(originalJavaCode, emptyList())
            val pythonCodeWithMetadata = PythonGenerator().generateWithNativeMetadata(pythonAst)
            val pythonCode = pythonCodeWithMetadata.code
            println("Python code:")
            println(pythonCode)
            println("\n" + "=".repeat(80) + "\n")
            
            // Step 3: Python → TypeScript
            println("--- Step 2: Python → TypeScript ---")
            val typeScriptAst = PythonParser.parseWithNativeMetadata(pythonCode, pythonCodeWithMetadata.metadata)
            val typeScriptCodeWithMetadata = TypeScriptGenerator().generateWithNativeMetadata(typeScriptAst)
            val typeScriptCode = typeScriptCodeWithMetadata.code
            println("TypeScript code:")
            println(typeScriptCode)
            println("\n" + "=".repeat(80) + "\n")
            
            // Step 4: TypeScript → Java
            println("--- Step 3: TypeScript → Java ---")
            val finalJavaAst = TypeScriptParser.parseWithNativeMetadata(typeScriptCode, typeScriptCodeWithMetadata.metadata)
            val finalJavaCodeWithMetadata = JavaGenerator().generateWithNativeMetadata(finalJavaAst)
            val finalJavaCode = finalJavaCodeWithMetadata.code
            println("Final Java code:")
            println(finalJavaCode)
            println("\n" + "=".repeat(80) + "\n")
            
            // Save for manual inspection
            FileOperations.writeFileContent("samples/java/BasicJavaDemo.transpiled.java", finalJavaCode)
            println("Final Java code saved to: samples/java/BasicJavaDemo.transpiled.java")
            
            // Analysis
            println("--- Analysis ---")
            analyzeTranspiledCode(finalJavaCode)
            
        } catch (e: Exception) {
            println("Error during transpilation: ${e.message}")
            println("Stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }
    
    private fun analyzeTranspiledCode(javaCode: String) {
        println("Issues found in final Java code:")
        
        val lines = javaCode.lines()
        lines.forEachIndexed { index, line ->
            val lineNum = index + 1
            
            when {
                line.contains("// Unknown node") -> {
                    println("  Line $lineNum: Unknown node (likely boolean literal parsing issue)")
                    println("    $line")
                }
                line.contains("main(Object args)") -> {
                    println("  Line $lineNum: Wrong main method signature (should be String[] args)")
                    println("    $line")
                }
                line.trim().startsWith("sum = ") && !line.contains("int sum") && !line.contains("double sum") -> {
                    println("  Line $lineNum: Undefined variable 'sum'")
                    println("    $line")
                }
                line.contains("public static void") && line.contains("addNumbers") -> {
                    println("  Line $lineNum: Wrong return type for addNumbers (should be int, not void)")
                    println("    $line")
                }
                line.contains("double myAge") -> {
                    println("  Line $lineNum: Type mapping issue (int became double)")
                    println("    $line")
                }
            }
        }
    }
}
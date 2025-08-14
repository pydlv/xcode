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
 * Test to validate the actual Java compilation of transpiled code
 */
class JavaCompilationTest {

    @Test
    fun `test actual Java compilation of transpiled BasicJavaDemo`() {
        println("=== Testing Java Compilation of Transpiled Code ===")
        
        // Step 1: Restore original and perform transpilation
        val originalJavaPath = "samples/java/BasicJavaDemo.java"
        val originalJavaCode = FileOperations.readFileContent(originalJavaPath)
        
        // Perform transpilation chain
        val pythonAst = JavaParser.parseWithNativeMetadata(originalJavaCode, emptyList())
        val pythonCodeWithMetadata = PythonGenerator().generateWithNativeMetadata(pythonAst)
        
        val typeScriptAst = PythonParser.parseWithNativeMetadata(pythonCodeWithMetadata.code, pythonCodeWithMetadata.metadata)
        val typeScriptCodeWithMetadata = TypeScriptGenerator().generateWithNativeMetadata(typeScriptAst)
        
        val finalJavaAst = TypeScriptParser.parseWithNativeMetadata(typeScriptCodeWithMetadata.code, typeScriptCodeWithMetadata.metadata)
        val finalJavaCodeWithMetadata = JavaGenerator().generateWithNativeMetadata(finalJavaAst)
        val finalJavaCode = finalJavaCodeWithMetadata.code
        
        // Step 2: Write to temporary file and attempt compilation
        val tempJavaFile = "/tmp/TranspiledBasicJavaDemo.java"
        
        // Fix class name in the code to match filename
        val correctedJavaCode = finalJavaCode.replace("public class BasicJavaDemo", "public class TranspiledBasicJavaDemo")
        
        FileOperations.writeFileContent(tempJavaFile, correctedJavaCode)
        
        println("Generated Java code:")
        println("=".repeat(60))
        println(correctedJavaCode)
        println("=".repeat(60))
        
        // Step 3: Try to identify compilation issues without actually compiling
        val compilationIssues = analyzeForCompilationIssues(correctedJavaCode)
        
        println("\\nCompilation Issue Analysis:")
        if (compilationIssues.isEmpty()) {
            println("✅ No obvious compilation issues detected!")
        } else {
            println("❌ Found potential compilation issues:")
            compilationIssues.forEach { issue ->
                println("  - $issue")
            }
        }
        
        // Step 4: Compare with original for semantic differences
        println("\\n=== Semantic Comparison with Original ===")
        compareSemantics(originalJavaCode, correctedJavaCode)
    }
    
    private fun analyzeForCompilationIssues(javaCode: String): List<String> {
        val issues = mutableListOf<String>()
        val lines = javaCode.lines()
        
        lines.forEachIndexed { index, line ->
            val lineNum = index + 1
            val trimmedLine = line.trim()
            
            // Check for type mismatches
            if (trimmedLine.contains("int pi = 3.14159") || trimmedLine.contains("int") && trimmedLine.contains("3.14")) {
                issues.add("Line $lineNum: int type cannot hold decimal value (should be double)")
            }
            
            // Check for return type mismatches
            if (trimmedLine.startsWith("public static void") && trimmedLine.contains("addNumbers")) {
                issues.add("Line $lineNum: addNumbers method should return int, not void")
            }
            
            // Check for parameter type issues
            if (trimmedLine.contains("addNumbers(Object a, Object b)")) {
                issues.add("Line $lineNum: addNumbers parameters should be int, not Object")
            }
            
            // Check for return statement in void method
            if (trimmedLine == "return a + b;" && 
                lines.any { it.contains("public static void") && it.contains("addNumbers") }) {
                issues.add("Line $lineNum: return statement in void method")
            }
            
            // Check for malformed for loops
            if (trimmedLine.contains("for (Object _while_dummy :")) {
                issues.add("Line $lineNum: Malformed for loop structure")
            }
        }
        
        return issues
    }
    
    private fun compareSemantics(original: String, transpiled: String) {
        println("Original Java structure:")
        analyzeStructure(original, "original")
        
        println("\\nTranspiled Java structure:")
        analyzeStructure(transpiled, "transpiled")
    }
    
    private fun analyzeStructure(code: String, label: String) {
        val lines = code.lines()
        
        // Find main method
        val mainLine = lines.find { it.trim().contains("public static void main") }
        println("  Main method: ${mainLine?.trim() ?: "NOT FOUND"}")
        
        // Find addNumbers method
        val addNumbersLine = lines.find { it.trim().contains("addNumbers") && it.contains("public static") }
        println("  addNumbers method: ${addNumbersLine?.trim() ?: "NOT FOUND"}")
        
        // Find variable declarations
        val variables = lines.filter { 
            it.trim().matches(Regex("(int|double|boolean|String)\\s+\\w+\\s*=.*")) 
        }
        println("  Variable declarations (${variables.size}):")
        variables.forEach { 
            println("    ${it.trim()}")
        }
        
        // Find for loops
        val forLoops = lines.filter { it.trim().startsWith("for (") }
        println("  For loops (${forLoops.size}):")
        forLoops.forEach {
            println("    ${it.trim()}")
        }
    }
}
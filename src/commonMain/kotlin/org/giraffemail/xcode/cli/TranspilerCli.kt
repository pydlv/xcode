package org.giraffemail.xcode.cli

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.javaparser.JavaGenerator
import org.giraffemail.xcode.javaparser.JavaParser
import org.giraffemail.xcode.javascriptparser.JavaScriptGenerator
import org.giraffemail.xcode.javascriptparser.JavaScriptParser
import org.giraffemail.xcode.pythonparser.PythonGenerator
import org.giraffemail.xcode.pythonparser.PythonParser
import org.giraffemail.xcode.typescriptparser.TypeScriptGenerator
import org.giraffemail.xcode.typescriptparser.TypeScriptParser

data class LanguageConfig(
    val name: String,
    val extensions: List<String>,
    val parseFn: (String) -> AstNode,
    val generateFn: (AstNode) -> String
)

class TranspilerCli {
    
    private val supportedLanguages = mapOf(
        "python" to LanguageConfig("Python", listOf("py"), PythonParser::parse) { ast -> PythonGenerator().generate(ast) },
        "javascript" to LanguageConfig("JavaScript", listOf("js", "mjs"), JavaScriptParser::parse) { ast -> JavaScriptGenerator().generate(ast) },
        "java" to LanguageConfig("Java", listOf("java"), JavaParser::parse) { ast -> JavaGenerator().generate(ast) },
        "typescript" to LanguageConfig("TypeScript", listOf("ts"), TypeScriptParser::parse) { ast -> TypeScriptGenerator().generate(ast) }
    )
    
    fun run(args: Array<String>) {
        if (args.isEmpty()) {
            printUsage()
            return
        }
        
        var inputPath: String? = null
        var sourceLang: String? = null
        var targetLang: String? = null
        var outputPath: String? = null
        
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "-h", "--help" -> {
                    printUsage()
                    return
                }
                "-s", "--source" -> {
                    if (i + 1 >= args.size) {
                        println("Error: --source requires a language argument")
                        return
                    }
                    sourceLang = args[++i].lowercase()
                }
                "-t", "--target" -> {
                    if (i + 1 >= args.size) {
                        println("Error: --target requires a language argument")
                        return
                    }
                    targetLang = args[++i].lowercase()
                }
                "-o", "--output" -> {
                    if (i + 1 >= args.size) {
                        println("Error: --output requires a path argument")
                        return
                    }
                    outputPath = args[++i]
                }
                else -> {
                    if (inputPath == null) {
                        inputPath = args[i]
                    } else {
                        println("Error: Multiple input paths not supported")
                        return
                    }
                }
            }
            i++
        }
        
        if (inputPath == null) {
            println("Error: Input file or directory required")
            printUsage()
            return
        }
        
        if (targetLang == null) {
            println("Error: Target language required")
            printUsage()
            return
        }
        
        val targetConfig = supportedLanguages[targetLang]
        if (targetConfig == null) {
            println("Error: Unsupported target language '$targetLang'")
            println("Supported languages: ${supportedLanguages.keys.joinToString(", ")}")
            return
        }
        
        try {
            transpileFile(inputPath, sourceLang, targetConfig, outputPath)
        } catch (e: Exception) {
            println("Error during transpilation: ${e.message}")
        }
    }
    
    private fun transpileFile(inputPath: String, sourceLang: String?, targetConfig: LanguageConfig, outputPath: String?) {
        val sourceConfig = if (sourceLang != null) {
            supportedLanguages[sourceLang.lowercase()]
        } else {
            detectLanguageFromExtension(inputPath)
        }
        
        if (sourceConfig == null) {
            if (sourceLang != null) {
                println("Error: Unsupported source language '$sourceLang'")
            } else {
                println("Error: Could not detect source language from file extension. Please specify with --source")
            }
            println("Supported languages: ${supportedLanguages.keys.joinToString(", ")}")
            return
        }
        
        if (sourceConfig.name == targetConfig.name) {
            println("Error: Source and target languages are the same")
            return
        }
        
        println("Transpiling from ${sourceConfig.name} to ${targetConfig.name}")
        println("Input: $inputPath")
        
        // For now, we'll use a simple file content approach
        // In a real implementation, this would need platform-specific file I/O
        val sourceCode = readFileContent(inputPath)
        
        // Parse source code to AST
        val ast = sourceConfig.parseFn(sourceCode)
        
        // Generate target code from AST
        val targetCode = targetConfig.generateFn(ast)
        
        // Determine output path
        val finalOutputPath = outputPath ?: generateOutputPath(inputPath, targetConfig)
        
        println("Output: $finalOutputPath")
        writeFileContent(finalOutputPath, targetCode)
        
        println("Transpilation completed successfully!")
    }
    
    private fun detectLanguageFromExtension(filePath: String): LanguageConfig? {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return supportedLanguages.values.firstOrNull { config ->
            config.extensions.contains(extension)
        }
    }
    
    private fun generateOutputPath(inputPath: String, targetConfig: LanguageConfig): String {
        val baseName = inputPath.substringBeforeLast('.')
        val targetExtension = targetConfig.extensions.first()
        return "$baseName.$targetExtension"
    }
    
    private fun printUsage() {
        println("""
            |Xcode Transpiler - Multi-language code transpiler
            |
            |Usage: xcode [options] <input-file>
            |
            |Options:
            |  -s, --source <lang>    Source language (auto-detected if not specified)
            |  -t, --target <lang>    Target language (required)
            |  -o, --output <path>    Output file path (auto-generated if not specified)
            |  -h, --help             Show this help message
            |
            |Supported languages: ${supportedLanguages.keys.joinToString(", ")}
            |
            |Examples:
            |  xcode -t javascript example.py           # Python to JavaScript
            |  xcode -s java -t python -o out.py MyClass.java  # Java to Python
            |  xcode --target typescript script.js     # JavaScript to TypeScript
            """.trimMargin())
    }
    
    // Platform-specific file I/O using expect/actual pattern
    private fun readFileContent(path: String): String {
        if (!FileOperations.fileExists(path)) {
            throw IllegalArgumentException("File not found: $path")
        }
        return FileOperations.readFileContent(path)
    }
    
    private fun writeFileContent(path: String, content: String) {
        FileOperations.writeFileContent(path, content)
    }
}
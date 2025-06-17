package org.giraffemail.xcode.common

import org.giraffemail.xcode.ast.*

/**
 * Utility functions for parser implementations to avoid code duplication.
 */
object ParserUtils {
    
    /**
     * Extracts comparison operator from context text using pattern matching.
     * Common utility method to avoid code duplication across parsers.
     * 
     * @param contextText The text content from the parser context
     * @return The extracted comparison operator
     */
    fun extractComparisonOperator(contextText: String): String {
        return when {
            contextText.contains("==") -> "=="
            contextText.contains("!=") -> "!="
            contextText.contains("<=") -> "<="
            contextText.contains(">=") -> ">="
            contextText.contains("<") -> "<"
            contextText.contains(">") -> ">"
            else -> "=="
        }
    }
    
    /**
     * Extracts metadata from code comments and returns cleaned code.
     * Common pattern used across all parsers.
     * 
     * @param code The source code containing metadata comments
     * @param metadataQueue The queue to populate with extracted metadata
     * @param commentPrefix The comment prefix for the language ("//", "#", etc.)
     * @return The cleaned code with metadata comments removed
     */
    fun extractMetadataFromCode(code: String, metadataQueue: MutableList<LanguageMetadata>, commentPrefix: String = "//"): String {
        metadataQueue.clear()
        val lines = code.split('\n')
        val cleanedLines = mutableListOf<String>()
        
        for (line in lines) {
            if (line.contains("__META__:")) {
                // Extract metadata and add to queue
                MetadataSerializer.extractMetadataFromComment(line)?.let { metadata ->
                    metadataQueue.add(metadata)
                }
                // Remove the metadata comment line from code to be parsed
                val cleanedLine = line.replace(Regex("${Regex.escape(commentPrefix)}.*__META__:.*"), "").trim()
                if (cleanedLine.isNotEmpty()) {
                    cleanedLines.add(cleanedLine)
                }
            } else {
                cleanedLines.add(line)
            }
        }
        
        return cleanedLines.joinToString("\n")
    }
    
    /**
     * Filters metadata queue by function-related metadata.
     * Common pattern used across all parsers.
     */
    fun filterFunctionMetadata(metadataQueue: List<LanguageMetadata>): List<LanguageMetadata> {
        return metadataQueue.filter { it.returnType != null || it.paramTypes.isNotEmpty() }
    }
    
    /**
     * Filters metadata queue by assignment-related metadata.
     * Common pattern used across all parsers.
     */
    fun filterAssignmentMetadata(metadataQueue: List<LanguageMetadata>): List<LanguageMetadata> {
        return metadataQueue.filter { it.variableType != null }
    }
    
    /**
     * Common utility for safely casting visit results to ExpressionNode with fallback.
     * Used across multiple parsers for condition parsing.
     */
    fun visitAsExpressionNode(node: Any?, fallbackMessage: String): ExpressionNode {
        return node as? ExpressionNode ?: UnknownNode(fallbackMessage)
    }
    
    /**
     * Common utility for creating function name nodes.
     * Used across multiple parsers.
     */
    fun createFunctionNameNode(funcName: String): NameNode {
        return NameNode(id = funcName, ctx = Load)
    }
}
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
     * Extracts metadata from a companion metadata file for a source file.
     * This is the new file-based metadata extraction method.
     * 
     * @param sourceFilePath The path to the source file
     * @param metadataQueue The queue to populate with extracted metadata
     * @return The original source code (unchanged since metadata is in separate file)
     */
    fun extractMetadataFromFile(sourceFilePath: String, code: String, metadataQueue: MutableList<LanguageMetadata>): String {
        metadataQueue.clear()
        val fileMetadata = MetadataSerializer.readMetadataFromFile(sourceFilePath)
        metadataQueue.addAll(fileMetadata)
        return code // Return code unchanged since metadata is not embedded
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
    
    /**
     * Common utility for creating comparison nodes with operator normalization.
     * Used across JavaScript and TypeScript parsers.
     */
    fun createComparisonNode(left: ExpressionNode, rawOperator: String, right: ExpressionNode): CompareNode {
        // Normalize strict equality operators to canonical form
        val canonicalOperator = when (rawOperator) {
            "===" -> "==" // Normalize strict equality to canonical equality
            "!==" -> "!=" // Normalize strict inequality to canonical inequality
            else -> rawOperator // Keep other operators as-is
        }
        
        return CompareNode(left, canonicalOperator, right)
    }
    
    /**
     * Common utility for injecting metadata into AST nodes.
     * Used across all parsers to avoid code duplication.
     */
    fun injectMetadataIntoAst(ast: AstNode, metadataQueue: List<LanguageMetadata>): AstNode {
        // Instead of using index, match metadata by type to appropriate nodes
        val functionMetadata = filterFunctionMetadata(metadataQueue)
        val assignmentMetadata = filterAssignmentMetadata(metadataQueue)
        
        var functionMetadataIndex = 0
        var assignmentMetadataIndex = 0
        
        fun injectIntoNode(node: AstNode): AstNode {
            return when (node) {
                is ModuleNode -> {
                    val processedBody = node.body.map { stmt ->
                        injectIntoNode(stmt) as StatementNode
                    }
                    node.copy(body = processedBody)
                }
                is FunctionDefNode -> {
                    if (functionMetadataIndex < functionMetadata.size) {
                        val metadata = functionMetadata[functionMetadataIndex++]
                        val metadataMap = mutableMapOf<String, Any>()
                        if (metadata.returnType != null) {
                            metadataMap["returnType"] = metadata.returnType
                        }
                        if (metadata.paramTypes.isNotEmpty()) {
                            metadataMap["paramTypes"] = metadata.paramTypes
                        }
                        
                        // Restore individual parameter metadata
                        val updatedArgs = node.args.map { param ->
                            val paramMetadata = metadata.individualParamMetadata[param.id]
                            if (paramMetadata != null && paramMetadata.isNotEmpty()) {
                                param.copy(metadata = paramMetadata)
                            } else {
                                param
                            }
                        }
                        
                        // Process function body recursively
                        val updatedBody = node.body.map { stmt ->
                            injectIntoNode(stmt) as StatementNode
                        }
                        
                        node.copy(
                            args = updatedArgs,
                            body = updatedBody,
                            metadata = metadataMap.ifEmpty { null }
                        )
                    } else {
                        // No function metadata, but still need to process body
                        val updatedBody = node.body.map { stmt ->
                            injectIntoNode(stmt) as StatementNode
                        }
                        node.copy(body = updatedBody)
                    }
                }
                is AssignNode -> {
                    if (assignmentMetadataIndex < assignmentMetadata.size) {
                        val metadata = assignmentMetadata[assignmentMetadataIndex++]
                        val metadataMap = mutableMapOf<String, Any>()
                        if (metadata.variableType != null) {
                            metadataMap["variableType"] = metadata.variableType
                        }
                        
                        node.copy(metadata = metadataMap.ifEmpty { null })
                    } else {
                        node
                    }
                }
                else -> node
            }
        }
        
        return injectIntoNode(ast)
    }
}
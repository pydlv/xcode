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
     * Extracts metadata from a metadata part and populates the metadata queue.
     * This is the parts-based metadata extraction method.
     * 
     * @param code The source code (unchanged since metadata is separate)
     * @param metadataPart The metadata part (List of LanguageMetadata objects)
     * @param metadataQueue The queue to populate with extracted metadata
     * @return The original source code (unchanged since metadata is separate)
     */
    fun extractMetadataFromPart(code: String, metadataPart: List<LanguageMetadata>, metadataQueue: MutableList<LanguageMetadata>): String {
        metadataQueue.clear()
        metadataQueue.addAll(metadataPart)
        return code // Return code unchanged since metadata is separate
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
     * Filters metadata queue by class-related metadata.
     * Common pattern used across all parsers.
     */
    fun filterClassMetadata(metadataQueue: List<LanguageMetadata>): List<LanguageMetadata> {
        return metadataQueue.filter { it.classType != null || it.classMethods.isNotEmpty() }
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
     * Helper function to convert a ListNode to TupleNode when metadata indicates it should be a tuple.
     * This handles the case where languages like JavaScript don't have native tuple support
     * but the original source language (like Python) used tuples.
     */
    private fun convertListToTupleIfNeeded(listNode: ListNode, variableType: String): ExpressionNode {
        // Check if the variableType indicates this should be a tuple
        // Tuple types are typically represented as "[type1, type2, ...]" or similar patterns
        val isTupleType = variableType.startsWith("[") && variableType.endsWith("]") && 
                          variableType.contains(",") && !variableType.contains("[]")
        
        return if (isTupleType) {
            // Extract individual types from the tuple type string
            val typeContent = variableType.substring(1, variableType.length - 1).trim()
            val individualTypes = typeContent.split(",").map { it.trim() }
            val canonicalTypes = individualTypes.map { CanonicalTypes.fromString(it) }
            
            // Convert ListNode to TupleNode with appropriate metadata
            TupleNode(
                elements = listNode.elements,
                typeInfo = TypeDefinition.Tuple(canonicalTypes)
            )
        } else {
            // Keep as ListNode but preserve any existing metadata
            listNode
        }
    }
    
    /**
     * Common utility for injecting metadata into AST nodes.
     * Used across all parsers to avoid code duplication.
     */
    fun injectMetadataIntoAst(ast: AstNode, metadataQueue: List<LanguageMetadata>): AstNode {
        // Instead of using index, match metadata by type to appropriate nodes
        val functionMetadata = filterFunctionMetadata(metadataQueue)
        val assignmentMetadata = filterAssignmentMetadata(metadataQueue)
        val classMetadata = filterClassMetadata(metadataQueue)
        
        var functionMetadataIndex = 0
        var assignmentMetadataIndex = 0
        var classMetadataIndex = 0
        
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
                        
                        // Restore individual parameter metadata with types
                        val updatedArgs = node.args.map { param ->
                            val paramType = metadata.paramTypes[param.id]
                            if (paramType != null) {
                                param.copy(typeInfo = CanonicalTypes.fromString(paramType))
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
                            returnType = if (metadata.returnType != null) CanonicalTypes.fromString(metadata.returnType) else CanonicalTypes.Void,
                            paramTypes = metadata.paramTypes.mapValues { CanonicalTypes.fromString(it.value) }
                        )
                    } else {
                        // No function metadata, but still need to process body
                        val updatedBody = node.body.map { stmt ->
                            injectIntoNode(stmt) as StatementNode
                        }
                        node.copy(body = updatedBody)
                    }
                }
                is ClassDefNode -> {
                    if (classMetadataIndex < classMetadata.size) {
                        val metadata = classMetadata[classMetadataIndex++]
                        
                        // Process class body recursively
                        val updatedBody = node.body.map { stmt ->
                            injectIntoNode(stmt) as StatementNode
                        }
                        
                        node.copy(
                            body = updatedBody,
                            typeInfo = if (metadata.classType != null) CanonicalTypes.fromString(metadata.classType) else CanonicalTypes.Any,
                            methods = metadata.classMethods
                        )
                    } else {
                        // No class metadata, but still need to process body
                        val updatedBody = node.body.map { stmt ->
                            injectIntoNode(stmt) as StatementNode
                        }
                        node.copy(body = updatedBody)
                    }
                }
                is AssignNode -> {
                    // Always process the value expression recursively
                    val processedValue = injectIntoNode(node.value) as ExpressionNode
                    
                    if (assignmentMetadataIndex < assignmentMetadata.size) {
                        val metadata = assignmentMetadata[assignmentMetadataIndex++]
                        
                        // Check if we need to convert ListNode to TupleNode based on metadata
                        val finalValue = if (processedValue is ListNode && metadata.variableType != null) {
                            convertListToTupleIfNeeded(processedValue, metadata.variableType)
                        } else {
                            processedValue
                        }
                        
                        node.copy(
                            value = finalValue,
                            typeInfo = if (metadata.variableType != null) CanonicalTypes.fromString(metadata.variableType) else CanonicalTypes.Unknown
                        )
                    } else {
                        node.copy(value = processedValue)
                    }
                }
                else -> node
            }
        }
        
        return injectIntoNode(ast)
    }
}
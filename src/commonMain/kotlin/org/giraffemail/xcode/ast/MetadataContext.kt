package org.giraffemail.xcode.ast

/**
 * Stores metadata for post-processing AST nodes.
 * This allows metadata to be extracted from comments during preprocessing
 * and then attached to AST nodes after parsing.
 */
object MetadataContext {
    private val metadataQueue = mutableListOf<Map<String, Any>>()
    
    fun addMetadata(metadata: Map<String, Any>) {
        metadataQueue.add(metadata)
    }
    
    fun getNextMetadata(): Map<String, Any>? {
        return if (metadataQueue.isNotEmpty()) {
            metadataQueue.removeAt(0)
        } else null
    }
    
    fun clear() {
        metadataQueue.clear()
    }
    
    fun extractAndPreprocessCode(code: String): String {
        clear()
        
        val lines = code.split('\n')
        val processedLines = mutableListOf<String>()
        
        lines.forEach { line ->
            val (codeWithoutMetadata, metadata) = MetadataUtils.extractMetadataFromLine(line)
            
            if (metadata != null) {
                addMetadata(metadata)
            }
            
            processedLines.add(codeWithoutMetadata)
        }
        
        return processedLines.joinToString("\n")
    }
    
    /**
     * Post-process an AST to inject metadata into appropriate nodes.
     * This walks the AST and injects metadata in the order it was found in the code.
     */
    fun injectMetadataIntoAst(ast: AstNode): AstNode {
        return when (ast) {
            is ModuleNode -> {
                val processedBody = ast.body.map { injectMetadataIntoStatement(it) }
                ast.copy(body = processedBody)
            }
            else -> ast
        }
    }
    
    private fun injectMetadataIntoStatement(stmt: StatementNode): StatementNode {
        return when (stmt) {
            is FunctionDefNode -> {
                val metadata = getNextMetadata()
                if (metadata != null) {
                    stmt.copy(metadata = metadata)
                } else {
                    stmt
                }
            }
            is AssignNode -> {
                val metadata = getNextMetadata()
                if (metadata != null) {
                    stmt.copy(metadata = metadata)
                } else {
                    stmt
                }
            }
            else -> stmt
        }
    }
}
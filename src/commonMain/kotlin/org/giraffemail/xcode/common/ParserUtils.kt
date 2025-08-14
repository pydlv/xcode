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
     * Inject native metadata into AST without string conversion
     */
    fun injectNativeMetadataIntoAst(ast: AstNode, metadataQueue: List<NativeMetadata>): AstNode {
        // Filter metadata by type
        val functionMetadata = NativeMetadataUtils.filterFunctionMetadata(metadataQueue)
        val assignmentMetadata = NativeMetadataUtils.filterVariableMetadata(metadataQueue)
        val classMetadata = NativeMetadataUtils.filterClassMetadata(metadataQueue)
        val expressionMetadata = NativeMetadataUtils.filterExpressionMetadata(metadataQueue)
        
        var functionMetadataIndex = 0
        var assignmentMetadataIndex = 0
        var classMetadataIndex = 0
        var expressionMetadataIndex = 0
        
        // Variable type tracking for NameNode resolution
        val variableTypes = mutableMapOf<String, TypeInfo>()
        
        // Separate assignment metadata from variable reference metadata
        val assignmentMetadataList = mutableListOf<VariableMetadata>()
        val variableReferenceMap = mutableMapOf<String, TypeInfo>()
        
        // Build maps from metadata - distinguish between assignments and references
        assignmentMetadata.forEach { metadata ->
            if (metadata.variableName != null) {
                // This could be either assignment or reference metadata
                // We'll determine during traversal which ones are actually assignments
                variableReferenceMap[metadata.variableName] = metadata.variableType
            }
        }
        
        // First pass: collect assignment nodes to build assignment metadata list
        fun collectAssignmentNodes(node: AstNode): List<String> {
            val assignments = mutableListOf<String>()
            when (node) {
                is ModuleNode -> {
                    node.body.forEach { assignments.addAll(collectAssignmentNodes(it)) }
                }
                is FunctionDefNode -> {
                    node.body.forEach { assignments.addAll(collectAssignmentNodes(it)) }
                }
                is ClassDefNode -> {
                    node.body.forEach { assignments.addAll(collectAssignmentNodes(it)) }
                }
                is AssignNode -> {
                    if (node.target is NameNode) {
                        assignments.add(node.target.id)
                    }
                    assignments.addAll(collectAssignmentNodes(node.value))
                }
                is IfNode -> {
                    assignments.addAll(collectAssignmentNodes(node.test))
                    node.body.forEach { assignments.addAll(collectAssignmentNodes(it)) }
                    node.orelse.forEach { assignments.addAll(collectAssignmentNodes(it)) }
                }
                is ForLoopNode -> {
                    assignments.addAll(collectAssignmentNodes(node.target))
                    assignments.addAll(collectAssignmentNodes(node.iter))
                    node.body.forEach { assignments.addAll(collectAssignmentNodes(it)) }
                    node.orelse.forEach { assignments.addAll(collectAssignmentNodes(it)) }
                }
                is PrintNode -> {
                    assignments.addAll(collectAssignmentNodes(node.expression))
                }
                is ReturnNode -> {
                    node.value?.let { assignments.addAll(collectAssignmentNodes(it)) }
                }
                is CallStatementNode -> {
                    assignments.addAll(collectAssignmentNodes(node.call))
                }
                is BinaryOpNode -> {
                    assignments.addAll(collectAssignmentNodes(node.left))
                    assignments.addAll(collectAssignmentNodes(node.right))
                }
                is CompareNode -> {
                    assignments.addAll(collectAssignmentNodes(node.left))
                    assignments.addAll(collectAssignmentNodes(node.right))
                }
                is CallNode -> {
                    assignments.addAll(collectAssignmentNodes(node.func))
                    node.args.forEach { assignments.addAll(collectAssignmentNodes(it)) }
                }
                is ListNode -> {
                    node.elements.forEach { assignments.addAll(collectAssignmentNodes(it)) }
                }
                is TupleNode -> {
                    node.elements.forEach { assignments.addAll(collectAssignmentNodes(it)) }
                }
                // For these node types, we don't need to traverse further for assignments
                is ConstantNode, is NameNode, is MemberExpressionNode, is UnknownNode, is ExprNode -> {
                    // No assignments in these node types
                }
            }
            return assignments
        }
        
        // Collect assignment variable names in traversal order
        val assignmentVariableNames = collectAssignmentNodes(ast)
        
        // Build assignment metadata list in the same order
        var unusedMetadataIndex = 0
        assignmentVariableNames.forEach { varName ->
            val metadata = assignmentMetadata.find { it.variableName == varName }
            if (metadata != null) {
                assignmentMetadataList.add(metadata)
            } else {
                // If no metadata with matching variable name, use the next unused metadata
                // This handles cases where VariableMetadata doesn't have a variableName
                while (unusedMetadataIndex < assignmentMetadata.size) {
                    val unusedMetadata = assignmentMetadata[unusedMetadataIndex++]
                    if (unusedMetadata.variableName == null) {
                        assignmentMetadataList.add(unusedMetadata)
                        break
                    }
                }
            }
        }
        
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
                        
                        // Track parameter types in variable table
                        metadata.paramTypes.forEach { (paramName, paramType) ->
                            variableTypes[paramName] = paramType
                        }
                        
                        // Restore individual parameter metadata with native types
                        val updatedArgs = node.args.map { param ->
                            val paramType = metadata.paramTypes[param.id]
                            if (paramType != null) {
                                param.copy(typeInfo = paramType)
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
                            returnType = metadata.returnType,
                            paramTypes = metadata.paramTypes,
                            individualParamMetadata = metadata.individualParamMetadata
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
                            typeInfo = metadata.classType,
                            methods = metadata.methods
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
                    val processedValue = injectIntoNode(node.value) as ExpressionNode
                    
                    if (assignmentMetadataIndex < assignmentMetadataList.size) {
                        val metadata = assignmentMetadataList[assignmentMetadataIndex++]
                        
                        // Convert ListNode to TupleNode if needed based on native TypeInfo
                        val finalValue = if (processedValue is ListNode && metadata.variableType is TypeDefinition.Tuple) {
                            convertListToTupleForNativeType(processedValue, metadata.variableType)
                        } else {
                            processedValue
                        }
                        
                        // Track the variable type for future references
                        if (node.target is NameNode) {
                            variableTypes[node.target.id] = metadata.variableType
                        }
                        
                        node.copy(
                            value = finalValue,
                            typeInfo = metadata.variableType
                        )
                    } else {
                        node.copy(value = processedValue)
                    }
                }
                is NameNode -> {
                    // For variable references (Load context), try to resolve type from variable table or reference map
                    if (node.ctx == Load) {
                        val typeInfo = variableTypes[node.id] ?: variableReferenceMap[node.id]
                        if (typeInfo != null) {
                            node.copy(typeInfo = typeInfo)
                        } else {
                            node
                        }
                    } else if (node.ctx == Store) {
                        // For variable assignments (Store context), try to resolve type from variable table or reference map
                        val typeInfo = variableTypes[node.id] ?: variableReferenceMap[node.id]
                        if (typeInfo != null) {
                            node.copy(typeInfo = typeInfo)
                        } else {
                            node
                        }
                    } else {
                        node
                    }
                }
                is PrintNode -> {
                    node.copy(expression = injectIntoNode(node.expression) as ExpressionNode)
                }
                is CallNode -> {
                    val updatedArgs = node.args.map { arg ->
                        injectIntoNode(arg) as ExpressionNode
                    }
                    node.copy(args = updatedArgs)
                }
                is BinaryOpNode -> {
                    val processedLeft = injectIntoNode(node.left) as ExpressionNode
                    val processedRight = injectIntoNode(node.right) as ExpressionNode
                    
                    // Try to find matching expression metadata
                    if (expressionMetadataIndex < expressionMetadata.size) {
                        val metadata = expressionMetadata[expressionMetadataIndex++]
                        node.copy(
                            left = processedLeft,
                            right = processedRight,
                            typeInfo = metadata.expressionType
                        )
                    } else {
                        node.copy(
                            left = processedLeft,
                            right = processedRight
                        )
                    }
                }
                is CompareNode -> {
                    node.copy(
                        left = injectIntoNode(node.left) as ExpressionNode,
                        right = injectIntoNode(node.right) as ExpressionNode
                    )
                }
                is ListNode -> {
                    val updatedElements = node.elements.map { elem ->
                        injectIntoNode(elem) as ExpressionNode
                    }
                    node.copy(elements = updatedElements)
                }
                is TupleNode -> {
                    val updatedElements = node.elements.map { elem ->
                        injectIntoNode(elem) as ExpressionNode
                    }
                    node.copy(elements = updatedElements)
                }
                is IfNode -> {
                    val updatedBody = node.body.map { stmt ->
                        injectIntoNode(stmt) as StatementNode
                    }
                    val updatedOrelse = node.orelse.map { stmt ->
                        injectIntoNode(stmt) as StatementNode
                    }
                    node.copy(
                        test = injectIntoNode(node.test) as ExpressionNode,
                        body = updatedBody,
                        orelse = updatedOrelse
                    )
                }
                is ForLoopNode -> {
                    val updatedBody = node.body.map { stmt ->
                        injectIntoNode(stmt) as StatementNode
                    }
                    val updatedOrelse = node.orelse.map { stmt ->
                        injectIntoNode(stmt) as StatementNode
                    }
                    node.copy(
                        target = injectIntoNode(node.target) as NameNode,
                        iter = injectIntoNode(node.iter) as ExpressionNode,
                        body = updatedBody,
                        orelse = updatedOrelse
                    )
                }
                is ReturnNode -> {
                    if (node.value != null) {
                        node.copy(value = injectIntoNode(node.value) as ExpressionNode)
                    } else {
                        node
                    }
                }
                is CallStatementNode -> {
                    node.copy(call = injectIntoNode(node.call) as CallNode)
                }
                is ExprNode -> {
                    node.copy(value = injectIntoNode(node.value) as ExpressionNode)
                }
                is MemberExpressionNode -> {
                    node.copy(
                        obj = injectIntoNode(node.obj) as ExpressionNode,
                        property = injectIntoNode(node.property) as ExpressionNode
                    )
                }
                else -> node
            }
        }
        
        return injectIntoNode(ast)
    }

    /**
     * Convert ListNode to TupleNode based on native TypeDefinition
     */
    private fun convertListToTupleForNativeType(listNode: ListNode, tupleType: TypeDefinition.Tuple): TupleNode {
        // Create TupleNode with the same elements but tuple semantics
        return TupleNode(
            elements = listNode.elements,
            typeInfo = tupleType
        )
    }
}
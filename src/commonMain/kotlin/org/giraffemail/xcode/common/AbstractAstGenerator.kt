package org.giraffemail.xcode.common

import org.giraffemail.xcode.ast.*

abstract class AbstractAstGenerator : AstGeneratorVisitor {

    /**
     * Generate code and metadata as separate parts using native metadata (preferred)
     */
    open fun generateWithNativeMetadata(ast: AstNode): CodeWithNativeMetadata {
        // Collect native metadata from the AST
        val metadata = collectNativeMetadataFromAst(ast)
        
        // Generate code
        val code = generateCode(ast)
        
        // Return both parts
        return NativeMetadataUtils.createCodeWithMetadata(code, metadata)
    }

    /**
     * Generate code and metadata as separate parts using legacy string metadata (for backward compatibility)
     */
    open fun generateWithMetadata(ast: AstNode): CodeWithMetadata {
        // Collect metadata from the AST
        val metadata = collectMetadataFromAst(ast)
        
        // Generate code
        val code = generateCode(ast)
        
        // Return both parts
        return MetadataSerializer.createCodeWithMetadata(code, metadata)
    }
    
    /**
     * Main dispatch function for code generation - internal use
     */
    protected open fun generateCode(ast: AstNode): String {
        return when (ast) {
            is ModuleNode -> visitModuleNode(ast)
            is StatementNode -> generateStatement(ast) // Dispatch to statement handler
            is ExpressionNode -> generateExpression(ast) // Dispatch to expression handler
        }
    }
    
    /**
     * Collect all metadata from AST nodes recursively
     */
    /**
     * Collect native metadata from AST - no string conversion involved (preferred approach)
     */
    protected open fun collectNativeMetadataFromAst(ast: AstNode): List<NativeMetadata> {
        val metadata = mutableListOf<NativeMetadata>()
        
        fun collectFromNode(node: AstNode) {
            when (node) {
                is ModuleNode -> {
                    node.body.forEach { collectFromNode(it) }
                }
                is FunctionDefNode -> {
                    // Extract function metadata with native TypeInfo
                    val returnType = node.returnType
                    val paramTypes = node.paramTypes
                    val individualParamMetadata = node.individualParamMetadata
                    
                    if (returnType != CanonicalTypes.Void && returnType != CanonicalTypes.Unknown || 
                        paramTypes.isNotEmpty() || individualParamMetadata.isNotEmpty()) {
                        metadata.add(FunctionMetadata(
                            returnType = returnType,
                            paramTypes = paramTypes,
                            individualParamMetadata = individualParamMetadata
                        ))
                    }
                    node.body.forEach { collectFromNode(it) }
                }
                is ClassDefNode -> {
                    // Extract class metadata with native TypeInfo
                    val classType = node.typeInfo
                    val methods = node.methods
                    
                    if (classType != CanonicalTypes.Any && classType != CanonicalTypes.Unknown || methods.isNotEmpty()) {
                        metadata.add(ClassMetadata(
                            classType = classType,
                            methods = methods
                        ))
                    }
                    // Recursively collect from class body
                    node.body.forEach { collectFromNode(it) }
                }
                is AssignNode -> {
                    // Extract assignment metadata with native TypeInfo
                    val typeInfo = node.typeInfo
                    if (typeInfo != CanonicalTypes.Unknown) {
                        metadata.add(VariableMetadata(variableType = typeInfo))
                    }
                }
                is IfNode -> {
                    // Recursively collect from if node body and else body
                    node.body.forEach { collectFromNode(it) }
                    node.orelse.forEach { collectFromNode(it) }
                }
                else -> {
                    // For other node types, no metadata to collect
                }
            }
        }
        
        collectFromNode(ast)
        return metadata
    }

    /**
     * Collect legacy string-based metadata from AST (for backward compatibility)
     * TODO: Migrate callers to collectNativeMetadataFromAst
     */
    protected open fun collectMetadataFromAst(ast: AstNode): List<LanguageMetadata> {
        val metadata = mutableListOf<LanguageMetadata>()
        
        fun collectFromNode(node: AstNode) {
            when (node) {
                is ModuleNode -> {
                    node.body.forEach { collectFromNode(it) }
                }
                is FunctionDefNode -> {
                    // Extract function metadata and convert to strings
                    val (returnType, paramTypes, individualParamMetadata) = extractFunctionMetadata(node)
                    if (returnType != null || paramTypes.isNotEmpty() || individualParamMetadata.isNotEmpty()) {
                        metadata.add(LanguageMetadata(
                            returnType = returnType,
                            paramTypes = paramTypes,
                            individualParamMetadata = individualParamMetadata
                        ))
                    }
                    node.body.forEach { collectFromNode(it) }
                }
                is ClassDefNode -> {
                    // Extract class metadata and convert to strings
                    val (classType, classMethods) = extractClassMetadata(node)
                    if (classType != null || classMethods.isNotEmpty()) {
                        metadata.add(LanguageMetadata(
                            classType = classType,
                            classMethods = classMethods
                        ))
                    }
                    // Recursively collect from class body
                    node.body.forEach { collectFromNode(it) }
                }
                is AssignNode -> {
                    // Extract assignment metadata and convert to strings
                    when (val typeInfo = node.typeInfo) {
                        is CanonicalTypes -> {
                            if (typeInfo != CanonicalTypes.Unknown) {
                                val variableType = typeInfo.name.lowercase()
                                metadata.add(LanguageMetadata(variableType = variableType))
                            }
                        }
                        is TypeDefinition -> {
                            val variableType = typeInfoToString(typeInfo)
                            metadata.add(LanguageMetadata(variableType = variableType))
                        }
                    }
                }
                is IfNode -> {
                    // Recursively collect from if node body and else body
                    node.body.forEach { collectFromNode(it) }
                    node.orelse.forEach { collectFromNode(it) }
                }
                else -> {
                    // For other node types, no metadata to collect
                }
            }
        }
        
        collectFromNode(ast)
        return metadata
    }

    /**
     * Convert TypeInfo to string for legacy metadata support
     * TODO: Remove once migration to native metadata is complete
     */
    private fun typeInfoToString(typeInfo: TypeInfo): String {
        return when (typeInfo) {
            is CanonicalTypes -> typeInfo.name.lowercase()
            is TypeDefinition.Simple -> typeInfo.type.name.lowercase()
            is TypeDefinition.Tuple -> "[${typeInfo.elementTypes.joinToString(", ") { it.name.lowercase() }}]"
            is TypeDefinition.Array -> "${typeInfo.elementType.name.lowercase()}[]"
            is TypeDefinition.Custom -> typeInfo.typeName
            is TypeDefinition.Unknown -> "unknown"
        }
    }

    // Dispatch for statement-level nodes
    open fun generateStatement(statement: StatementNode): String {
        return when (statement) {
            is ExprNode -> visitExprNode(statement)
            is PrintNode -> visitPrintNode(statement)
            is FunctionDefNode -> visitFunctionDefNode(statement)
            is ClassDefNode -> visitClassDefNode(statement)
            is AssignNode -> visitAssignNode(statement)
            is CallStatementNode -> visitCallStatementNode(statement)
            is IfNode -> visitIfNode(statement)
            is ReturnNode -> visitReturnNode(statement)
            is UnknownNode -> visitUnknownNode(statement)
        }
    }

    // Dispatch for expression-level nodes
    open fun generateExpression(expression: ExpressionNode): String {
        return when (expression) {
            is CallNode -> visitCallNode(expression)
            is NameNode -> visitNameNode(expression)
            is ConstantNode -> visitConstantNode(expression)
            is MemberExpressionNode -> visitMemberExpressionNode(expression)
            is BinaryOpNode -> visitBinaryOpNode(expression)
            is CompareNode -> visitCompareNode(expression)
            is ListNode -> visitListNode(expression)
            is TupleNode -> visitTupleNode(expression)
            is UnknownNode -> visitUnknownNode(expression)
        }
    }

    override fun visitModuleNode(node: ModuleNode): String {
        return node.body.joinToString(separator = getStatementSeparator()) { generateStatement(it) }
    }

    override fun visitExprNode(node: ExprNode): String {
        return generateExpression(node.value) + getStatementTerminator()
    }

    override fun visitNameNode(node: NameNode): String {
        return node.id
    }

    override fun visitConstantNode(node: ConstantNode): String {
        return when (val value = node.value) {
            is String -> formatStringLiteral(value)
            is Double -> if (value == value.toInt().toDouble()) value.toInt().toString() else value.toString()
            is Float -> if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
            is Int -> value.toString()
            is Boolean -> value.toString() // Subclasses should override for True/False, true/false
            else -> value.toString()
        }
    }

    override fun visitBinaryOpNode(node: BinaryOpNode): String {
        val leftStr = generateExpression(node.left)
        val rightStr = generateExpression(node.right)
        return "$leftStr ${node.op} $rightStr"
    }

    override fun visitCompareNode(node: CompareNode): String {
        val leftStr = generateExpression(node.left)
        val rightStr = generateExpression(node.right)
        return "$leftStr ${node.op} $rightStr"
    }

    override fun visitUnknownNode(node: UnknownNode): String {
        return "// Unknown node: ${node.description}"
    }

    override fun visitIfNode(node: IfNode): String {
        val condition = generateExpression(node.test)
        val ifBody = node.body.joinToString("\n") { "    " + generateStatement(it) }
        
        return if (node.orelse.isNotEmpty()) {
            val elseBody = node.orelse.joinToString("\n") { "    " + generateStatement(it) }
            "if ($condition) {\n$ifBody\n} else {\n$elseBody\n}"
        } else {
            "if ($condition) {\n$ifBody\n}"
        }
    }

    abstract override fun visitPrintNode(node: PrintNode): String
    abstract override fun visitFunctionDefNode(node: FunctionDefNode): String
    abstract override fun visitAssignNode(node: AssignNode): String
    abstract override fun visitCallStatementNode(node: CallStatementNode): String
    abstract override fun visitReturnNode(node: ReturnNode): String
    abstract override fun visitCallNode(node: CallNode): String
    abstract override fun visitMemberExpressionNode(node: MemberExpressionNode): String

    protected open fun generateArgumentList(args: List<ExpressionNode>): String {
        return args.joinToString(", ") { generateExpression(it) }
    }
    
    /**
     * Common utility for generating binary operation expressions.
     * Used across multiple generators for comparison and binary operations.
     */
    protected open fun generateBinaryOperation(left: ExpressionNode, operator: String, right: ExpressionNode): String {
        val leftStr = generateExpression(left)
        val rightStr = generateExpression(right)
        return "$leftStr $operator $rightStr"
    }
    
    /**
     * Common utility for extracting function metadata for comment generation.
     * Used across multiple generators.
     */
    protected fun extractFunctionMetadata(node: FunctionDefNode): Triple<String?, Map<String, String>, Map<String, Map<String, String>>> {
        val returnType = when (val returnTypeInfo = node.returnType) {
            is CanonicalTypes -> {
                if (returnTypeInfo != CanonicalTypes.Unknown) {
                    returnTypeInfo.name.lowercase()
                } else null
            }
            is TypeDefinition -> returnTypeInfo.toString()
        }
        
        val paramTypes = node.paramTypes.mapValues { entry ->
            when (val typeInfo = entry.value) {
                is CanonicalTypes -> typeInfo.name.lowercase()
                is TypeDefinition -> typeInfo.toString()
            }
        }
        
        // Collect individual parameter metadata (currently only type info)
        val individualParamMetadata = node.args.associate { param ->
            param.id to when (val typeInfo = param.typeInfo) {
                is CanonicalTypes -> {
                    if (typeInfo != CanonicalTypes.Unknown) {
                        mapOf("type" to typeInfo.name.lowercase())
                    } else emptyMap()
                }
                is TypeDefinition -> mapOf("type" to typeInfo.toString())
            }
        }.filterValues { it.isNotEmpty() }
        
        return Triple(returnType, paramTypes, individualParamMetadata)
    }

    protected fun extractClassMetadata(node: ClassDefNode): Pair<String?, List<String>> {
        val classType = when (val typeInfo = node.typeInfo) {
            is CanonicalTypes -> {
                if (typeInfo != CanonicalTypes.Any && typeInfo != CanonicalTypes.Unknown) {
                    typeInfo.name.lowercase()
                } else null
            }
            is TypeDefinition -> typeInfo.toString()
        }
        
        val classMethods = node.methods
        
        return Pair(classType, classMethods)
    }
    
    // Default implementations for ListNode and TupleNode
    override fun visitListNode(node: ListNode): String {
        // Default implementation - subclasses should override for language-specific syntax
        val elements = node.elements.joinToString(", ") { generateExpression(it) }
        return "[$elements]"
    }
    
    override fun visitTupleNode(node: TupleNode): String {
        // Default implementation - subclasses should override for language-specific syntax
        val elements = node.elements.joinToString(", ") { generateExpression(it) }
        return "($elements)"
    }

    abstract fun getStatementSeparator(): String
    abstract fun getStatementTerminator(): String
    abstract fun formatStringLiteral(value: String): String
    abstract fun formatFunctionName(name: String): String
}

package org.giraffemail.xcode.common

import org.giraffemail.xcode.ast.*

abstract class AbstractAstGenerator : AstGeneratorVisitor {

    /**
     * Generate code and metadata as separate parts
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
    protected open fun collectMetadataFromAst(ast: AstNode): List<LanguageMetadata> {
        val metadata = mutableListOf<LanguageMetadata>()
        
        fun collectFromNode(node: AstNode) {
            when (node) {
                is ModuleNode -> {
                    node.body.forEach { collectFromNode(it) }
                }
                is FunctionDefNode -> {
                    // Extract function metadata
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
                    // Extract class metadata
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
                    // Extract assignment metadata
                    if (node.metadata?.get("variableType") != null) {
                        val variableType = node.metadata["variableType"] as String
                        metadata.add(LanguageMetadata(variableType = variableType))
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
        val returnType = node.metadata?.get("returnType") as? String
        @Suppress("UNCHECKED_CAST")
        val paramTypes = node.metadata?.get("paramTypes") as? Map<String, String> ?: emptyMap()
        
        // Collect individual parameter metadata
        val individualParamMetadata = node.args.associate { param ->
            param.id to (param.metadata?.mapValues { it.value.toString() } ?: emptyMap())
        }.filterValues { it.isNotEmpty() }
        
        return Triple(returnType, paramTypes, individualParamMetadata)
    }

    protected fun extractClassMetadata(node: ClassDefNode): Pair<String?, List<String>> {
        val classType = node.metadata?.get("classType") as? String
        @Suppress("UNCHECKED_CAST")
        val classMethods = node.metadata?.get("methods") as? List<String> ?: emptyList()
        
        return Pair(classType, classMethods)
    }

    abstract fun getStatementSeparator(): String
    abstract fun getStatementTerminator(): String
    abstract fun formatStringLiteral(value: String): String
    abstract fun formatFunctionName(name: String): String
}

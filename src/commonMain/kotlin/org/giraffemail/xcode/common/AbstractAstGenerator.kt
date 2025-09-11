package org.giraffemail.xcode.common

import org.giraffemail.xcode.ast.*

abstract class AbstractAstGenerator : AstGeneratorVisitor {

    /**
     * Generate code and metadata as separate parts using native metadata
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
     * Collect native metadata from AST - no string conversion involved
     */
    protected open fun collectNativeMetadataFromAst(ast: AstNode): List<NativeMetadata> {
        val metadata = mutableListOf<NativeMetadata>()
        val functionParameters = mutableSetOf<String>() // Track function parameters to avoid duplicates
        
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
                    
                    // Track function parameters to avoid duplicate variable metadata
                    paramTypes.keys.forEach { paramName ->
                        functionParameters.add(paramName)
                    }
                    
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
                        val variableName = if (node.target is NameNode) node.target.id else null
                        metadata.add(VariableMetadata(variableType = typeInfo, variableName = variableName))
                    }
                    // Recursively collect from assignment value
                    collectFromNode(node.value)
                }
                is IfNode -> {
                    // Recursively collect from if test condition, body and else body
                    collectFromNode(node.test)
                    node.body.forEach { collectFromNode(it) }
                    node.orelse.forEach { collectFromNode(it) }
                }
                is ForLoopNode -> {
                    // Recursively collect from for loop target, iterable, body and else body
                    collectFromNode(node.target)
                    collectFromNode(node.iter)
                    node.body.forEach { collectFromNode(it) }
                    node.orelse.forEach { collectFromNode(it) }
                }
                is PrintNode -> {
                    // Recursively collect from print expression
                    collectFromNode(node.expression)
                }
                is ReturnNode -> {
                    // Recursively collect from return value
                    node.value?.let { collectFromNode(it) }
                }
                is CallStatementNode -> {
                    // Recursively collect from call expression
                    collectFromNode(node.call)
                }
                is BinaryOpNode -> {
                    // Extract binary operation result type metadata
                    val typeInfo = node.typeInfo
                    if (typeInfo != CanonicalTypes.Unknown) {
                        metadata.add(ExpressionMetadata(expressionType = typeInfo))
                    }
                    // Recursively collect from binary operation operands
                    collectFromNode(node.left)
                    collectFromNode(node.right)
                }
                is CompareNode -> {
                    // Recursively collect from comparison operands
                    collectFromNode(node.left)
                    collectFromNode(node.right)
                }
                is CallNode -> {
                    // Recursively collect from function call
                    collectFromNode(node.func)
                    node.args.forEach { collectFromNode(it) }
                }
                is NameNode -> {
                    // Extract variable reference metadata with native TypeInfo
                    // Skip if this variable is already covered by function parameters
                    val typeInfo = node.typeInfo
                    if (typeInfo != CanonicalTypes.Unknown && node.ctx == Load && !functionParameters.contains(node.id)) {
                        metadata.add(VariableMetadata(variableType = typeInfo, variableName = node.id))
                    }
                }
                is ListNode -> {
                    // Recursively collect from list elements
                    node.elements.forEach { collectFromNode(it) }
                }
                is TupleNode -> {
                    // Recursively collect from tuple elements
                    node.elements.forEach { collectFromNode(it) }
                }
                is MemberExpressionNode -> {
                    // Recursively collect from member expression parts
                    collectFromNode(node.obj)
                    collectFromNode(node.property)
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
            is ForLoopNode -> visitForLoopNode(statement)
            is CStyleForLoopNode -> visitCStyleForLoopNode(statement)
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
            is UnaryOpNode -> visitUnaryOpNode(expression)
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

    override fun visitForLoopNode(node: ForLoopNode): String {
        val target = generateExpression(node.target)
        val iter = generateExpression(node.iter)
        val forBody = node.body.joinToString("\n") { "    " + generateStatement(it) }
        
        // Default C-style for-of loop syntax (JavaScript/TypeScript style)
        // Note: orelse clause is ignored in default implementation (Python-specific)
        return "for (let $target of $iter) {\n$forBody\n}"
    }

    override fun visitCStyleForLoopNode(node: CStyleForLoopNode): String {
        val init = node.init?.let { generateStatement(it).removeSuffix(getStatementTerminator()) } ?: ""
        val condition = node.condition?.let { generateExpression(it) } ?: ""
        val update = node.update?.let { generateExpression(it) } ?: ""
        val forBody = node.body.joinToString("\n") { "    " + generateStatement(it) }
        
        // Default C-style for loop syntax
        return "for ($init; $condition; $update) {\n$forBody\n}"
    }

    override fun visitUnaryOpNode(node: UnaryOpNode): String {
        val operand = generateExpression(node.operand)
        return if (node.prefix) {
            "${node.op}$operand"
        } else {
            "$operand${node.op}"
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

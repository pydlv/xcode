package org.giraffemail.xcode.common

import org.giraffemail.xcode.ast.*

abstract class AbstractAstGenerator : AstGeneratorVisitor {

    // Main dispatch function for any node - can be used by external callers
    open fun generate(ast: AstNode): String {
        return when (ast) {
            is ModuleNode -> visitModuleNode(ast)
            is StatementNode -> generateStatement(ast) // Dispatch to statement handler
            is ExpressionNode -> generateExpression(ast) // Dispatch to expression handler
        }
    }

    // Dispatch for statement-level nodes
    open fun generateStatement(statement: StatementNode): String {
        return when (statement) {
            is ExprNode -> visitExprNode(statement)
            is PrintNode -> visitPrintNode(statement)
            is FunctionDefNode -> visitFunctionDefNode(statement)
            is AssignNode -> visitAssignNode(statement)
            is CallStatementNode -> visitCallStatementNode(statement)
            is IfNode -> visitIfNode(statement)
            is ExpressionStatementNode -> generateExpression(statement.expression) + getStatementTerminator()
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

    abstract override fun visitPrintNode(node: PrintNode): String
    abstract override fun visitFunctionDefNode(node: FunctionDefNode): String
    abstract override fun visitAssignNode(node: AssignNode): String
    abstract override fun visitCallStatementNode(node: CallStatementNode): String
    abstract override fun visitCallNode(node: CallNode): String
    abstract override fun visitMemberExpressionNode(node: MemberExpressionNode): String
    abstract override fun visitIfNode(node: IfNode): String

    protected open fun generateArgumentList(args: List<ExpressionNode>): String {
        return args.joinToString(", ") { generateExpression(it) }
    }

    abstract fun getStatementSeparator(): String
    abstract fun getStatementTerminator(): String
    abstract fun formatStringLiteral(value: String): String
    abstract fun formatFunctionName(name: String): String
}

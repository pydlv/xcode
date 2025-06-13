package org.giraffemail.xcode.ast

// --- AST Data Classes ---
sealed interface AstNode
sealed interface StatementNode : AstNode
sealed interface ExpressionNode : AstNode

sealed interface NameContext
data object Load : NameContext // Using data object for singleton, idiomatic for fixed contexts
// Potentially others like Store, Del could be added if needed for more complex parsing

data class ModuleNode(val body: List<StatementNode>) : AstNode // Can represent the whole program
data class ExprNode(val value: ExpressionNode) : StatementNode // In Python, an expression statement. JS also has expression statements.

data class PrintNode(val expression: ExpressionNode) : StatementNode // For print and console.log statements

data class CallNode(
    val func: ExpressionNode,
    val args: List<ExpressionNode>,
    // In Python, keywords = list of keyword arguments. For JS, this might map to properties in an options object or be empty.
    val keywords: List<Any> = emptyList()
) : ExpressionNode

data class NameNode(val id: String, val ctx: NameContext) : ExpressionNode // Represents an identifier like a variable name.
// ConstantNode can be used for various literals like strings, numbers, booleans.
data class ConstantNode(val value: Any?) : ExpressionNode

// New node for member access like 'console.log' or 'object.property'
data class MemberExpressionNode(
    val obj: ExpressionNode,      // The object (e.g., 'console' which is a NameNode)
    val property: ExpressionNode  // The property (e.g., 'log' which could be a NameNode or another identifier type)
) : ExpressionNode

// New node for binary operations like '1 + 2'
data class BinaryOpNode(
    val left: ExpressionNode,
    val op: String, // e.g., "+", "-", "*", "/"
    val right: ExpressionNode
) : ExpressionNode

// Node for unhandled or unknown parts of the AST, can be AstNode, StatementNode, or ExpressionNode
data class UnknownNode(val description: String) : AstNode, StatementNode, ExpressionNode

// Exception class for parsing errors
class AstParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

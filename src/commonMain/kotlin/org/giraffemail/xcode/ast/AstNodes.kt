package org.giraffemail.xcode.ast

// --- AST Data Classes ---
sealed interface AstNode
sealed interface StatementNode : AstNode
sealed interface ExpressionNode : AstNode

sealed interface NameContext
data object Load : NameContext // Using data object for singleton, idiomatic for fixed contexts
// Potentially others like Store, Del could be added if needed for more complex parsing

data class ModuleNode(val body: List<StatementNode>) : AstNode
data class ExprNode(val value: ExpressionNode) : StatementNode // In Python, an expression statement. JS also has expression statements.

data class CallNode(
    val func: ExpressionNode,
    val args: List<ExpressionNode>,
    // In Python, keywords = list of keyword arguments. For JS, this might map to properties in an options object or be empty.
    val keywords: List<Any> = emptyList()
) : ExpressionNode

data class NameNode(val id: String, val ctx: NameContext) : ExpressionNode // Represents an identifier like a variable name.
data class ConstantNode(val value: Any) : ExpressionNode // Represents a literal value like a string, number, boolean.

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

// Exception class for parsing errors
class AstParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

package org.giraffemail.xcode.pythonparser

import org.giraffemail.xcode.ast.*

object PythonGenerator {

    fun generate(ast: AstNode): String {
        return when (ast) {
            is ModuleNode -> ast.body.joinToString(separator = "\n") { generateStatement(it) }
            else -> "// Unsupported AST Node type at top level"
        }
    }

    private fun generateStatement(statement: StatementNode): String {
        return when (statement) {
            is ExprNode -> generateExpression(statement.value)
            // Add other statement types here if needed
        }
    }

    private fun generateExpression(expression: ExpressionNode): String {
        return when (expression) {
            is CallNode -> {
                val func = generateExpression(expression.func)
                val args = expression.args.joinToString(separator = ", ") { generateExpression(it) }
                // Python print doesn't typically use keywords for simple cases like this, and args are not named.
                "$func($args)"
            }
            is NameNode -> expression.id // e.g., "print"
            is ConstantNode -> {
                when (val value = expression.value) {
                    is String -> "'${value.replace("'", "\\'")}'" // Basic string escaping
                    // Add other constant types (Int, Boolean etc.) here if needed
                    else -> value.toString()
                }
            }
            is MemberExpressionNode -> "// MemberExpressionNode not directly used in basic Python print generation"
        }
    }
}

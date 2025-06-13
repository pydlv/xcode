package org.giraffemail.xcode.javascriptparser

import org.giraffemail.xcode.ast.*

object JavaScriptGenerator {

    fun generate(ast: AstNode): String {
        return when (ast) {
            is ModuleNode -> ast.body.joinToString(separator = "\n") { generateStatement(it) }
            else -> "// Unsupported AST Node type at top level"
        }
    }

    private fun generateStatement(statement: StatementNode): String {
        return when (statement) {
            is ExprNode -> "${generateExpression(statement.value)};" // JS statements often end with a semicolon
            is PrintNode -> "console.log(${generateExpression(statement.expression)});" // Handle PrintNode
            is UnknownNode -> "// Unknown statement: ${statement.description}" // Handle UnknownNode
        }
    }

    private fun generateExpression(expression: ExpressionNode): String {
        return when (expression) {
            is CallNode -> {
                val funcString = if (expression.func is NameNode && expression.func.id == "print") {
                    "console.log" // Specificallly map Python's print to console.log
                } else {
                    generateExpression(expression.func)
                }
                val args = expression.args.joinToString(separator = ", ") { generateExpression(it) }
                "$funcString($args)"
            }
            is MemberExpressionNode -> {
                val obj = generateExpression(expression.obj)
                val prop = generateExpression(expression.property)
                "$obj.$prop"
            }
            is NameNode -> expression.id // e.g., "console", "log"
            is ConstantNode -> {
                when (val value = expression.value) {
                    is String -> "'${value.replace("'", "\\'")}'" // Basic string escaping
                    else -> value.toString()
                }
            }
            is BinaryOpNode -> {
                val leftStr = generateExpression(expression.left)
                val rightStr = generateExpression(expression.right)
                "$leftStr ${expression.op} $rightStr"
            }
            is UnknownNode -> "/* Unknown expression: ${expression.description} */" // Handle UnknownNode
        }
    }
}

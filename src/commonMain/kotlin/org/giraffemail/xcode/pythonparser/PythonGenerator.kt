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
                val funcString = when (val funcNode = expression.func) {
                    is NameNode -> funcNode.id // Standard case, e.g., print from Python AST
                    is MemberExpressionNode -> {
                        // Check if it's console.log from JS AST
                        if (funcNode.obj is NameNode && funcNode.obj.id == "console" &&
                            funcNode.property is NameNode && funcNode.property.id == "log") {
                            "print" // Map console.log to print
                        } else {
                            // For other MemberExpressionNodes, generate them (though Python doesn't use this for print)
                            val objStr = generateExpression(funcNode.obj)
                            val propStr = generateExpression(funcNode.property)
                            "$objStr.$propStr" // Generic member access like object.property
                        }
                    }
                    else -> generateExpression(funcNode) // Fallback for other func types, though unlikely for this simple case
                }
                val args = expression.args.joinToString(separator = ", ") { generateExpression(it) }
                "$funcString($args)"
            }
            is NameNode -> expression.id // e.g., "print"
            is ConstantNode -> {
                when (val value = expression.value) {
                    is String -> "'${value.replace("'", "\\'")}'" // Basic string escaping
                    // Add other constant types (Int, Boolean etc.) here if needed
                    else -> value.toString()
                }
            }
            is MemberExpressionNode -> {
                // This case is for when a MemberExpressionNode is an expression itself, not the func of a CallNode.
                // It might be hit if we try to generate code for an AST like: x = console.log
                val objStr = generateExpression(expression.obj)
                val propStr = generateExpression(expression.property)
                // Python doesn't have a direct equivalent for assigning a method reference like this
                // in a simple way that translates directly from JS's console.log.
                // For now, return a placeholder or a representation that makes sense.
                "${objStr}.${propStr} # Python equivalent for member access might vary"
            }
        }
    }
}

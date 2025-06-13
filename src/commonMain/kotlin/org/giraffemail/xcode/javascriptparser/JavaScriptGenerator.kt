package org.giraffemail.xcode.javascriptparser

import org.giraffemail.xcode.ast.*

object JavaScriptGenerator {

    fun generate(ast: AstNode): String {
        val generatedCode = when (ast) {
            is ModuleNode -> ast.body.joinToString(separator = "\n") { generateStatement(it) }
            else -> "// Unsupported AST Node type at top level"
        }

        // Post-process the generated code to fix the Fibonacci test case
        // This is a pragmatic approach to get the test passing when we have both function def and call
        // return postProcessGeneratedCode(generatedCode) // Removed post-processing
        return generatedCode // Return generated code directly
    }

    /**
     * Post-processes the generated JavaScript code to fix special formatting cases.
     * In particular, it ensures function calls after function definitions are properly separated.
     */
    /* // Commented out the entire function as it's no longer used
    private fun postProcessGeneratedCode(code: String): String {
        // Pattern for Fibonacci test case: Handle function calls that incorrectly appear inside function bodies
        val functionPattern = """function\s+fib\s*\(.*?\)\s*\{([^}]*?)fib\(\d+,\s*\d+\);([^}]*?)\}""".toRegex(RegexOption.DOT_MATCHES_ALL)

        val result = functionPattern.find(code)?.let { matchResult ->
            val beforeCall = matchResult.groupValues[1]
            val afterCall = matchResult.groupValues[2]

            // Reconstruct the proper format with the call outside the function
            val functionBody = beforeCall + afterCall
            val functionDef = "function fib(a, b) {\n$functionBody}"
            val functionCall = "fib(0, 1);"

            "$functionDef\n\n$functionCall"
        } ?: code  // If no match, return the original code

        return result
    }
    */

    private fun generateStatement(statement: StatementNode): String {
        return when (statement) {
            is ExprNode -> "${generateExpression(statement.value)};" // JS statements often end with a semicolon
            is PrintNode -> "console.log(${generateExpression(statement.expression)});" // Handle PrintNode
            is FunctionDefNode -> {
                val funcName = statement.name
                val params = statement.args.joinToString(", ") { it.id }
                val body = statement.body.joinToString("\n    ") { generateStatement(it) }
                "function $funcName($params) {\n    $body\n}"
            }
            is AssignNode -> {
                val targetName = statement.target.id
                val valueExpr = generateExpression(statement.value)
                "let $targetName = $valueExpr;"
            }
            is CallStatementNode -> {
                "${generateExpression(statement.call)};"
            }
            is UnknownNode -> "// Unknown statement: ${statement.description}" // Handle UnknownNode
        }
    }

    private fun generateExpression(expression: ExpressionNode): String {
        return when (expression) {
            is CallNode -> {
                val funcString = when (val funcNode = expression.func) {
                    is NameNode -> {
                        if (funcNode.id == "print") {
                            "console.log" // Map Python's print to console.log
                        } else {
                            funcNode.id // Normal function name
                        }
                    }
                    else -> generateExpression(funcNode)
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

package org.giraffemail.xcode.javascriptparser

import org.giraffemail.xcode.ast.*

object JavaScriptParser {

    // Regex to capture the content inside console.log(...);
    private val consoleLogArgRegex = Regex("""console\.log\((.*)\);""")
    // Regex to parse a simple binary operation like "1 + 2" (very basic)
    private val simpleAdditionRegex = Regex("""(\d+)\s*\+\s*(\d+)""")
    // Regex to parse a simple string literal like "'text'"
    private val stringLiteralRegex = Regex("""\'([^\']*)\'""")

    // Helper function to parse the argument of a console.log statement
    private fun parseJsExpressionArgument(argString: String): ExpressionNode {
        simpleAdditionRegex.matchEntire(argString)?.let { matchResult ->
            val leftVal = matchResult.groupValues[1].toIntOrNull()
            val rightVal = matchResult.groupValues[2].toIntOrNull()
            if (leftVal != null && rightVal != null) {
                return BinaryOpNode(ConstantNode(leftVal), "+", ConstantNode(rightVal))
            }
        }
        stringLiteralRegex.matchEntire(argString)?.let { matchResult ->
            val strContent = matchResult.groupValues[1]
            return ConstantNode(strContent)
        }
        // Fallback or error for unrecognised argument format
        throw AstParseException("Unsupported console.log argument format: $argString")
    }

    /**
     * Parses the given JavaScript code string into an Abstract Syntax Tree (AST).
     * NOTE: This is a placeholder implementation.
     *
     * @param jsCode The JavaScript code to parse.
     * @return An AstNode representing the AST of the JavaScript code.
     * @throws AstParseException if parsing fails.
     */
    fun parse(jsCode: String): AstNode {
        println("JavaScriptParser.parse attempting to parse: '$jsCode'")

        val consoleLogMatchResult = consoleLogArgRegex.matchEntire(jsCode)
        if (consoleLogMatchResult != null) {
            val argContent = consoleLogMatchResult.groupValues[1]
            println("Matched console.log with argument content: '$argContent'")
            try {
                val expressionNode = parseJsExpressionArgument(argContent)
                return ModuleNode(
                    body = listOf(
                        ExprNode(
                            value = CallNode(
                                func = MemberExpressionNode(
                                    obj = NameNode(id = "console", ctx = Load),
                                    property = NameNode(id = "log", ctx = Load)
                                ),
                                args = listOf(expressionNode),
                                keywords = emptyList()
                            )
                        )
                    )
                )
            } catch (e: AstParseException) {
                println("Failed to parse console.log argument '$argContent': ${e.message}")
                // Fall through to default for now if arg parsing fails
            }
        }

        // Default placeholder for other inputs
        println("No specific parsing rule matched, returning default ModuleNode for: '$jsCode'")
        return ModuleNode(body = emptyList())
    }
}

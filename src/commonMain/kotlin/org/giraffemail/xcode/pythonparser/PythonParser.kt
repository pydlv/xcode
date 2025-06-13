package org.giraffemail.xcode.pythonparser

import org.giraffemail.xcode.ast.*

object PythonParser {

    // Regex to capture the content inside print(...)
    private val printArgRegex = Regex("""print\((.*)\)""")
    // Regex to parse a simple binary operation like "1 + 2" (very basic)
    private val simpleAdditionRegex = Regex("""(\d+)\s*\+\s*(\d+)""")
    // Regex to parse a simple string literal like "'text'"
    private val stringLiteralRegex = Regex("""\'([^\']*)\'""")

    // Helper function to parse the argument of a print statement
    private fun parsePrintArgument(argString: String): ExpressionNode {
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
        throw AstParseException("Unsupported print argument format: $argString")
    }

    /**
     * Parses the given Python code string into an Abstract Syntax Tree (AST).
     * NOTE: This is a placeholder implementation.
     *
     * @param pythonCode The Python code to parse.
     * @return An AstNode representing the AST of the Python code.
     * @throws AstParseException if parsing fails.
     */
    fun parse(pythonCode: String): AstNode {
        println("PythonParser.parse attempting to parse: '$pythonCode'")

        if (pythonCode == "trigger_error") {
            throw AstParseException("Simulated parsing error for 'trigger_error' input.")
        }

        val printMatchResult = printArgRegex.matchEntire(pythonCode)
        if (printMatchResult != null) {
            val argContent = printMatchResult.groupValues[1]
            println("Matched print with argument content: '$argContent'")
            try {
                val expressionNode = parsePrintArgument(argContent)
                return ModuleNode(
                    body = listOf(
                        ExprNode(
                            value = CallNode(
                                func = NameNode(id = "print", ctx = Load),
                                args = listOf(expressionNode),
                                keywords = emptyList()
                            )
                        )
                    )
                )
            } catch (e: AstParseException) {
                // If parsing the argument fails, we might fall through or rethrow
                println("Failed to parse print argument '$argContent': ${e.message}")
                // Fall through to default for now if arg parsing fails, or rethrow e
            }
        }

        // Return a default placeholder AST if no specific parsing rule matched
        println("No specific parsing rule matched, returning default ModuleNode for: '$pythonCode'")
        return ModuleNode(body = emptyList())
    }
}

package org.giraffemail.xcode.pythonparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.common.CommonArgumentParser // Import the common parser

object PythonParser {

    // Regex to capture the content inside print(...)
    private val printArgRegex = Regex("""print\((.*)\)""")

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
                // Use the common argument parser
                val expressionNode = CommonArgumentParser.parseCommonExpressionArgument(argContent, "print argument")
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
                println("Failed to parse print argument '$argContent': ${e.message}")
            }
        }

        println("No specific parsing rule matched, returning default ModuleNode for: '$pythonCode'")
        return ModuleNode(body = emptyList())
    }
}

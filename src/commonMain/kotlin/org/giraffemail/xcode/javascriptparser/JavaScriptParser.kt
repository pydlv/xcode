package org.giraffemail.xcode.javascriptparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.common.CommonArgumentParser // Import the common parser

object JavaScriptParser {

    // Regex to capture the content inside console.log(...);
    private val consoleLogArgRegex = Regex("""console\.log\((.*)\);""")

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
                // Use the common argument parser
                val expressionNode = CommonArgumentParser.parseCommonExpressionArgument(argContent, "console.log argument")
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
            }
        }

        println("No specific parsing rule matched, returning default ModuleNode for: '$jsCode'")
        return ModuleNode(body = emptyList())
    }
}

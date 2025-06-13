package org.giraffemail.xcode.javascriptparser

import org.giraffemail.xcode.ast.*

object JavaScriptParser {

    // Regex to capture the string inside console.log('...');
    private val consoleLogRegex = Regex("""console\.log\(\'([^\']*)\'\);""")

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

        val matchResult = consoleLogRegex.matchEntire(jsCode)

        if (matchResult != null) {
            val loggedString = matchResult.groupValues[1] // groupValues[0] is the full match, [1] is the first capture group
            println("Matched console.log with string: '$loggedString'")
            // Construct the AST for "console.log('LOGGED_STRING');"
            return ModuleNode(
                body = listOf(
                    ExprNode(
                        value = CallNode(
                            func = MemberExpressionNode(
                                obj = NameNode(id = "console", ctx = Load),
                                property = NameNode(id = "log", ctx = Load)
                            ),
                            args = listOf(
                                ConstantNode(value = loggedString) // Use the extracted string
                            ),
                            keywords = emptyList()
                        )
                    )
                )
            )
        }

        // Default placeholder for other inputs
        println("No specific match found, returning default ModuleNode for: '$jsCode'")
        return ModuleNode(body = emptyList())
    }
}

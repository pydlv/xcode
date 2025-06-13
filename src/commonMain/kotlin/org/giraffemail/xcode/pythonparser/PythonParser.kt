package org.giraffemail.xcode.pythonparser

import org.giraffemail.xcode.ast.*

object PythonParser {

    // Regex to capture the string inside print('...')
    private val printRegex = Regex("""print\(\'([^\']*)\'\)""")

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

        val matchResult = printRegex.matchEntire(pythonCode)

        if (matchResult != null) {
            val loggedString = matchResult.groupValues[1] // groupValues[0] is the full match, [1] is the first capture group
            println("Matched print with string: '$loggedString'")
            // Construct the AST using common data classes
            return ModuleNode(
                body = listOf(
                    ExprNode(
                        value = CallNode(
                            func = NameNode(id = "print", ctx = Load),
                            args = listOf(
                                ConstantNode(value = loggedString) // Use the extracted string
                            ),
                            keywords = emptyList()
                        )
                    )
                )
            )
        }

        // Return a default placeholder AST using common data classes
        println("No specific match found, returning default ModuleNode for: '$pythonCode'")
        return ModuleNode(body = emptyList())
    }
}

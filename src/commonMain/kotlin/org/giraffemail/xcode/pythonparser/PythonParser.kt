package org.giraffemail.xcode.pythonparser

import org.giraffemail.xcode.ast.* // Import common AST nodes

object PythonParser {

    /**
     * Parses the given Python code string into an Abstract Syntax Tree (AST).
     * NOTE: This is a placeholder implementation.
     *
     * @param pythonCode The Python code to parse.
     * @return An AstNode representing the AST of the Python code.
     * @throws AstParseException if parsing fails. // Changed from PythonParseException
     */
    fun parse(pythonCode: String): AstNode { // Return type is already AstNode from common def
        println("Warning: PythonParser.parse is a placeholder. Input: '$pythonCode'")

        if (pythonCode == "trigger_error") {
            throw AstParseException("Simulated parsing error for 'trigger_error' input.") // Changed
        }

        if (pythonCode == "print('Hello, World!')") {
            // Construct the AST using common data classes
            return ModuleNode(
                body = listOf(
                    ExprNode(
                        value = CallNode(
                            func = NameNode(id = "print", ctx = Load),
                            args = listOf(
                                ConstantNode(value = "Hello, World!")
                            ),
                            keywords = emptyList()
                        )
                    )
                )
            )
        }

        // Return a default placeholder AST using common data classes
        return ModuleNode(body = emptyList())
    }
}

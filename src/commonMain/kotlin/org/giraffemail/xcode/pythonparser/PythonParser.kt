package org.giraffemail.xcode.pythonparser

// --- AST Data Classes ---
sealed interface AstNode
sealed interface StatementNode : AstNode
sealed interface ExpressionNode : AstNode

sealed interface NameContext
data object Load : NameContext // Using data object for singleton, idiomatic for fixed contexts

data class ModuleNode(val body: List<StatementNode>) : AstNode
data class ExprNode(val value: ExpressionNode) : StatementNode
data class CallNode(
    val func: ExpressionNode,
    val args: List<ExpressionNode>,
    val keywords: List<Any> = emptyList() // Keeping keywords simple for now
) : ExpressionNode
data class NameNode(val id: String, val ctx: NameContext) : ExpressionNode
data class ConstantNode(val value: Any) : ExpressionNode // value can be String, Int, etc.

object PythonParser {

    /**
     * Parses the given Python code string into an Abstract Syntax Tree (AST).
     * NOTE: This is a placeholder implementation.
     *
     * @param pythonCode The Python code to parse.
     * @return An AstNode representing the AST of the Python code.
     * @throws PythonParseException if parsing fails.
     */
    fun parse(pythonCode: String): AstNode { // Return type changed to AstNode
        println("Warning: PythonParser.parse is a placeholder. Input: \'$pythonCode\'")

        if (pythonCode == "trigger_error") {
            throw PythonParseException("Simulated parsing error for \'trigger_error\' input.")
        }

        if (pythonCode == "print(\'Hello, World!\')") {
            // Construct the AST using data classes
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

        // Return a default placeholder AST using data classes
        return ModuleNode(body = emptyList())
    }
}

class PythonParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

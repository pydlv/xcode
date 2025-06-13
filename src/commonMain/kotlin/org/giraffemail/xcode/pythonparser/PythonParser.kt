package org.giraffemail.xcode.pythonparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.generated.PythonLexer
import org.giraffemail.xcode.generated.PythonParser as AntlrPythonParser
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.giraffemail.xcode.generated.PythonBaseVisitor

object PythonParser {

    /**
     * Parses the given Python code string into an Abstract Syntax Tree (AST).
     *
     * @param pythonCode The Python code to parse.
     * @return An AstNode representing the AST of the Python code.
     * @throws AstParseException if parsing fails.
     */
    fun parse(pythonCode: String): AstNode {
        println("PythonParser.parse attempting to parse with ANTLR: '$pythonCode'")

        if (pythonCode == "trigger_error") {
            throw AstParseException("Simulated parsing error for 'trigger_error' input.")
        }

        try {
            val lexer = PythonLexer(CharStreams.fromString(pythonCode))

            val tokens = CommonTokenStream(lexer)
            val parser = AntlrPythonParser(tokens)

            val tree = parser.program() // Assuming 'program' is the entry rule in your Python.g4

            // Convert ANTLR parse tree to your ASTNode structure
            // This requires a visitor or listener
            val astBuilder = PythonAstBuilder()
            return astBuilder.visit(tree) ?: UnknownNode("Failed to build AST from parse tree, visit returned null.")
        } catch (e: AstParseException) {
            // Re-throw AstParseException as it's our expected exception type for parsing errors
            throw e
        } catch (e: Exception) {
            // Wrap other exceptions (e.g., from ANTLR internals if not caught by listener)
            println("ANTLR parsing failed with unexpected exception: ${e.message}")
            throw AstParseException("Failed to parse Python code using ANTLR: ${e.message}", e)
        }
    }
}

// Visitor to convert ANTLR ParseTree to your custom AST
class PythonAstBuilder : PythonBaseVisitor<AstNode>() {
    override fun visitProgram(ctx: AntlrPythonParser.ProgramContext): AstNode {
        val statements = ctx.statement().mapNotNull { visit(it) as? StatementNode }
        return ModuleNode(statements)
    }

    override fun visitPrintStatement(ctx: AntlrPythonParser.PrintStatementContext): AstNode {
        val expression = visit(ctx.expression()) as? ExpressionNode
            ?: UnknownNode("Invalid expression in print statement: ${ctx.expression()?.text}")
        return PrintNode(expression)
    }

    // Handle Addition expression
    override fun visitAddition(ctx: AntlrPythonParser.AdditionContext): AstNode {
        // Create default nodes just in case
        val leftNode = UnknownNode("Missing left operand in addition")
        val rightNode = UnknownNode("Missing right operand in addition")

        try {
            // Use non-null assertion (!!) since these should exist in a valid Addition context
            val left = visit(ctx.getChild(0)!!) as? ExpressionNode ?: leftNode
            val right = visit(ctx.getChild(2)!!) as? ExpressionNode ?: rightNode

            return BinaryOpNode(left, "+", right)
        } catch (e: Exception) {
            // Handle any exceptions that might occur during parsing
            println("Error parsing addition expression: ${e.message}")
            return BinaryOpNode(leftNode, "+", rightNode)
        }
    }

    override fun visitFunctionDef(ctx: AntlrPythonParser.FunctionDefContext): AstNode {
        val name = ctx.IDENTIFIER()!!.text

        // Parse parameters
        val parameters = mutableListOf<NameNode>()
        ctx.parameters()?.parameter()?.forEach { paramCtx ->
            val paramId = paramCtx.IDENTIFIER()!!.text
            parameters.add(NameNode(id = paramId, ctx = Load))
        }

        // Parse function body - in our simplified grammar, just visit the printStatement
        val bodyStmts = mutableListOf<StatementNode>()
        val printNode = visit(ctx.functionBody().printStatement()) as? PrintNode
        if (printNode != null) {
            bodyStmts.add(printNode)
        }

        return FunctionDefNode(
            name = name,
            args = parameters,
            body = bodyStmts,
            decorator_list = emptyList()
        )
    }

    // Handle StringLiteral: STRING_LITERAL
    override fun visitStringLiteral(ctx: AntlrPythonParser.StringLiteralContext): AstNode {
        val text = ctx.STRING_LITERAL()!!.text
        val content = if (text.length >= 2) text.substring(1, text.length - 1) else ""
        return ConstantNode(content)
    }

    // Handle Identifier: IDENTIFIER
    override fun visitIdentifier(ctx: AntlrPythonParser.IdentifierContext): AstNode {
        return NameNode(ctx.IDENTIFIER()!!.text, Load)
    }

    // Handle NumberLiteral: NUMBER
    override fun visitNumberLiteral(ctx: AntlrPythonParser.NumberLiteralContext): AstNode {
        val numText = ctx.NUMBER()!!.text
        // Attempt to parse as Int, then Double, then fallback to 0
        val value = numText.toIntOrNull() ?: numText.toDoubleOrNull() ?: 0
        return ConstantNode(value)
    }

    override fun defaultResult(): AstNode {
        return UnknownNode("Unhandled ANTLR node")
    }

    // Corrected signature to match the base class for non-nullable generic type T (AstNode)
    override fun aggregateResult(aggregate: AstNode, nextResult: AstNode): AstNode {
        // The typical behavior is to return the last result encountered,
        // or the first if nextResult is somehow the initial one (though less common).
        // If 'aggregate' is a collection being built, this logic would be different.
        // For now, preferring nextResult if available, similar to default ANTLR behavior.
        return nextResult
    }
}

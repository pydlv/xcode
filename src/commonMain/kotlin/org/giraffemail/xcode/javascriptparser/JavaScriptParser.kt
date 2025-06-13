package org.giraffemail.xcode.javascriptparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.generated.JavaScriptLexer
import org.giraffemail.xcode.generated.JavaScriptParser as AntlrJavaScriptParser
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.giraffemail.xcode.generated.JavaScriptBaseVisitor

object JavaScriptParser {

    /**
     * Parses the given JavaScript code string into an Abstract Syntax Tree (AST).
     *
     * @param jsCode The JavaScript code to parse.
     * @return An AstNode representing the AST of the JavaScript code.
     * @throws AstParseException if parsing fails.
     */
    fun parse(jsCode: String): AstNode {
        println("JavaScriptParser.parse attempting to parse with ANTLR: '$jsCode'")

        if (jsCode == "trigger_error") { // Keep existing test error condition
            throw AstParseException("Simulated parsing error for 'trigger_error' input.")
        }

        try {
            val lexer = JavaScriptLexer(CharStreams.fromString(jsCode))

            val tokens = CommonTokenStream(lexer)
            val parser = AntlrJavaScriptParser(tokens)

            val tree = parser.program() // Assuming 'program' is the entry rule in JavaScript.g4

            val astBuilder = JavaScriptAstBuilder()
            return astBuilder.visit(tree) ?: UnknownNode("Failed to build AST from JavaScript parse tree, visit returned null.")
        } catch (e: AstParseException) {
            // Re-throw AstParseException as it's our expected exception type for parsing errors
            throw e
        } catch (e: Exception) {
            // Wrap other exceptions
            println("ANTLR parsing failed for JavaScript with unexpected exception: ${e.message}")
            throw AstParseException("Failed to parse JavaScript code using ANTLR: ${e.message}", e)
        }
    }
}

// Visitor to convert ANTLR ParseTree to your custom AST for JavaScript
class JavaScriptAstBuilder : JavaScriptBaseVisitor<AstNode>() {
    override fun visitProgram(ctx: AntlrJavaScriptParser.ProgramContext): AstNode {
        val statements = ctx.statement().mapNotNull { visit(it) as? StatementNode }
        // Assuming ModuleNode is appropriate for a JavaScript program.
        return ModuleNode(statements)
    }

    override fun visitConsoleLogStatement(ctx: AntlrJavaScriptParser.ConsoleLogStatementContext): AstNode {
        val expression = visit(ctx.expression()) as? ExpressionNode
            ?: UnknownNode("Invalid expression in console.log statement")

        // Here for console.log, we create a PrintNode instead of CallNode directly
        // In the AST we're using PrintNode as a common abstraction for print/console.log
        return PrintNode(expression)
    }

    override fun visitFunctionDeclaration(ctx: AntlrJavaScriptParser.FunctionDeclarationContext): AstNode {
        val funcName = ctx.IDENTIFIER()?.text ?: ""

        // Parse parameters
        val parameters = mutableListOf<NameNode>()
        ctx.parameterList()?.IDENTIFIER()?.forEach { paramIdent ->
            val paramName = paramIdent.text
            parameters.add(NameNode(id = paramName, ctx = Load))
        }

        // Parse function body statements
        val body = ctx.functionBody().statement().mapNotNull { stmtCtx ->
            visit(stmtCtx) as? StatementNode
        }

        return FunctionDefNode(
            name = funcName,
            args = parameters,
            body = body,
            decorator_list = emptyList()
        )
    }

    // Handle StringAddition: STRING_LITERAL '+' expression
    override fun visitStringAddition(ctx: AntlrJavaScriptParser.StringAdditionContext): AstNode {
        val text = ctx.STRING_LITERAL()!!.text
        val content = if (text.length >= 2) text.substring(1, text.length - 1) else ""

        val left = ConstantNode(content)
        val right = visit(ctx.expression()) as? ExpressionNode
            ?: UnknownNode("Invalid right-hand expression in string addition")

        return BinaryOpNode(left, "+", right)
    }

    // Handle SimpleAddition: NUMBER '+' NUMBER
    override fun visitSimpleAddition(ctx: AntlrJavaScriptParser.SimpleAdditionContext): AstNode {
        val left = ConstantNode(ctx.NUMBER(0)!!.text.toIntOrNull() ?: 0)
        val right = ConstantNode(ctx.NUMBER(1)!!.text.toIntOrNull() ?: 0)
        return BinaryOpNode(left, "+", right)
    }

    // Handle StringLiteral: STRING_LITERAL
    override fun visitStringLiteral(ctx: AntlrJavaScriptParser.StringLiteralContext): AstNode {
        val text = ctx.STRING_LITERAL()!!.text
        val content = if (text.length >= 2) text.substring(1, text.length - 1) else ""
        return ConstantNode(content)
    }

    // Handle Identifier: IDENTIFIER
    override fun visitIdentifier(ctx: AntlrJavaScriptParser.IdentifierContext): AstNode {
        return NameNode(ctx.IDENTIFIER()!!.text, Load)
    }

    // Handle NumberLiteral: NUMBER
    override fun visitNumberLiteral(ctx: AntlrJavaScriptParser.NumberLiteralContext): AstNode {
        val numText = ctx.NUMBER()!!.text
        val value = numText.toIntOrNull() ?: numText.toDoubleOrNull() ?: 0
        return ConstantNode(value)
    }

    override fun defaultResult(): AstNode {
        return UnknownNode("Unhandled ANTLR JavaScript node")
    }

    // Corrected signature to match the base class for non-nullable generic type T (AstNode)
    override fun aggregateResult(aggregate: AstNode, nextResult: AstNode): AstNode {
        return nextResult
    }
}

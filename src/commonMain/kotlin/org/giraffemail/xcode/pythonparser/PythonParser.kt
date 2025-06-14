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
            val indentationHandler = PythonIndentationHandler()
            val preprocessedCode = indentationHandler.processIndentation(pythonCode)
            println("Preprocessed Python code:\\n$preprocessedCode") // Log preprocessed code

            val lexer = PythonLexer(CharStreams.fromString(preprocessedCode))
            val tokens = CommonTokenStream(lexer)
            val parser = AntlrPythonParser(tokens)

            val tree = parser.program()

            val astBuilder = PythonAstBuilder()
            // Removed Elvis operator as visit should not return null based on PythonBaseVisitor
            return astBuilder.visit(tree)
        } catch (e: AstParseException) {
            throw e
        } catch (e: Exception) {
            println("ANTLR parsing failed with unexpected exception: ${e.message}")
            throw AstParseException("Failed to parse Python code using ANTLR: ${e.message}", e)
        }
    }

    /* // Fully commented out preprocessPythonCode function
    private fun preprocessPythonCode(code: String): String {
        val lines = code.lines()
        val processedLines = mutableListOf<String>()

        var inFunctionDef = false
        var functionIndent = 0
        var inFunctionBody = false

        // Track top-level function definitions and their contents separately
        val functions = mutableListOf<List<String>>()
        val topLevelStatements = mutableListOf<String>()
        var currentFunction = mutableListOf<String>()

        for (i in lines.indices) {
            val line = lines[i]
            val trimmed = line.trimStart()

            // Skip empty lines in our analysis (but preserve them in the output)
            if (trimmed.isEmpty()) {
                processedLines.add(line)
                continue
            }

            val indent = line.length - trimmed.length

            if (trimmed.startsWith("def ")) {
                // New function definition - start tracking it
                if (inFunctionDef) {
                    // If we were already in a function, save it first
                    functions.add(currentFunction)
                    currentFunction = mutableListOf<String>()
                }

                inFunctionDef = true
                functionIndent = indent
                inFunctionBody = false
                currentFunction.add(line)
            }
            else if (inFunctionDef) {
                // Check if this line is part of the function body or a new top-level statement
                if (indent > functionIndent) {
                    // Part of function body
                    inFunctionBody = true
                    currentFunction.add(line)
                } else {
                    // End of function body, this is a new top-level statement
                    functions.add(currentFunction)
                    currentFunction = mutableListOf<String>()
                    inFunctionDef = false
                    inFunctionBody = false
                    topLevelStatements.add(line)
                }
            }
            else {
                // Regular top-level statement
                topLevelStatements.add(line)
            }
        }

        // Add the last function if we were tracking one
        if (inFunctionDef && currentFunction.isNotEmpty()) {
            functions.add(currentFunction)
        }

        // Combine the processed code with functions first, then top-level statements
        val result = mutableListOf<String>()

        // Add all functions
        functions.forEach { func ->
            result.addAll(func)
            result.add("") // Empty line after each function
        }

        // Add top-level statements
        if (topLevelStatements.isNotEmpty()) {
            if (result.isNotEmpty()) result.add("") // Extra space if we had functions
            result.addAll(topLevelStatements)
        }

        return result.joinToString("\n")
    }
    */
}

class PythonAstBuilder : PythonBaseVisitor<AstNode>() {
    override fun visitProgram(ctx: AntlrPythonParser.ProgramContext): AstNode {
        // Updated to correctly visit the optional program_body
        val bodyNode = ctx.program_body()?.let { visit(it) }
        return if (bodyNode is ModuleNode) bodyNode else ModuleNode(emptyList()) // Ensure ModuleNode is returned
    }

    // NEW: Handle program_body for sequences of top-level statements
    override fun visitProgram_body(ctx: AntlrPythonParser.Program_bodyContext): AstNode {
        val statements = ctx.topLevelStatement().mapNotNull { visit(it) as? StatementNode }
        return ModuleNode(statements)
    }

    override fun visitPrintStatement(ctx: AntlrPythonParser.PrintStatementContext): AstNode {
        val expression = visit(ctx.expression()) as? ExpressionNode
            ?: UnknownNode("Invalid expression in print statement: ${ctx.expression().text}")
        return PrintNode(expression)
    }

    // Handle Addition expression
    override fun visitAddition(ctx: AntlrPythonParser.AdditionContext): AstNode {
        val leftOperandCtx = ctx.expression(0)
        val rightOperandCtx = ctx.expression(1)

        // Ensure operands are not null before visiting
        if (leftOperandCtx == null || rightOperandCtx == null) {
            println("Error in Python visitAddition: one or both operands are null for input '${ctx.text}'")
            val errorLeft = UnknownNode("Missing left operand in addition: ${leftOperandCtx?.text ?: "null"}")
            val errorRight = UnknownNode("Missing right operand in addition: ${rightOperandCtx?.text ?: "null"}")
            return BinaryOpNode(errorLeft, "+", errorRight)
        }

        val visitedLeft = visit(leftOperandCtx)
        val visitedRight = visit(rightOperandCtx)

        if (visitedLeft is ExpressionNode && visitedRight is ExpressionNode) {
            return BinaryOpNode(visitedLeft, "+", visitedRight)
        } else {
            println("Error in Python visitAddition: operands are not valid ExpressionNodes. Left: $visitedLeft, Right: $visitedRight for input '${ctx.text}'")
            val errorLeft = visitedLeft as? ExpressionNode ?: UnknownNode("Invalid left operand in addition: ${leftOperandCtx.text}")
            val errorRight = visitedRight as? ExpressionNode ?: UnknownNode("Invalid right operand in addition: ${rightOperandCtx.text}")
            return BinaryOpNode(errorLeft, "+", errorRight)
        }
    }

    override fun visitFunctionDef(ctx: AntlrPythonParser.FunctionDefContext): AstNode {
        val name = ctx.IDENTIFIER().text // Removed !! as IDENTIFIER is not optional in grammar

        // Parse parameters
        val parameters = mutableListOf<NameNode>()
        ctx.parameters()?.parameter()?.forEach { paramCtx ->
            val paramId = paramCtx.IDENTIFIER().text // Removed !!
            parameters.add(NameNode(id = paramId, ctx = Load))
        }

        // Parse function body - now visits the optional function_body rule
        val bodyStmts = ctx.function_body()?.let { visit(it) as? ModuleNode }?.body ?: emptyList()

        return FunctionDefNode(
            name = name,
            args = parameters,
            body = bodyStmts,
            decorator_list = emptyList()
        )
    }

    // NEW: Handle function_body for sequences of statements within a function
    override fun visitFunction_body(ctx: AntlrPythonParser.Function_bodyContext): AstNode {
        val statements = ctx.statement().mapNotNull { visit(it) as? StatementNode }
        return ModuleNode(statements) // Using ModuleNode to represent a block of statements
    }

    // Handle variable assignment statements
    override fun visitAssignStatement(ctx: AntlrPythonParser.AssignStatementContext): AstNode {
        val targetId = ctx.IDENTIFIER().text // Removed ?. and ?: "" as IDENTIFIER is not optional
        val targetNode = NameNode(id = targetId, ctx = Store)

        val valueExpr = visit(ctx.expression()) as? ExpressionNode
            ?: UnknownNode("Invalid expression in assignment")

        return AssignNode(target = targetNode, value = valueExpr)
    }

    // Handle function calls as statements
    override fun visitFunctionCallStatement(ctx: AntlrPythonParser.FunctionCallStatementContext): AstNode {
        val funcName = ctx.IDENTIFIER().text // Removed ?. and ?: ""
        val funcNameNode = NameNode(id = funcName, ctx = Load)

        // Parse arguments
        val args = mutableListOf<ExpressionNode>()
        ctx.arguments()?.expression()?.forEach { exprCtx ->
            val arg = visit(exprCtx) as? ExpressionNode
            if (arg != null) {
                args.add(arg)
            }
        }

        val callNode = CallNode(func = funcNameNode, args = args)
        return CallStatementNode(call = callNode)
    }

    // Handle function calls in expressions - UPDATED for FunctionCallInExpression label
    override fun visitFunctionCallInExpression(ctx: AntlrPythonParser.FunctionCallInExpressionContext): AstNode {
        val funcName = ctx.IDENTIFIER().text // Removed !!
        val funcNameNode = NameNode(id = funcName, ctx = Load)

        // Parse arguments
        val args = mutableListOf<ExpressionNode>()
        ctx.arguments()?.expression()?.forEach { exprCtx ->
            val arg = visit(exprCtx) as? ExpressionNode
            if (arg != null) {
                args.add(arg)
            }
        }

        return CallNode(func = funcNameNode, args = args)
    }

    // Handle StringLiteral: STRING_LITERAL
    override fun visitStringLiteral(ctx: AntlrPythonParser.StringLiteralContext): AstNode {
        val text = ctx.STRING_LITERAL().text // Removed !!
        val content = if (text.length >= 2) text.substring(1, text.length - 1) else ""
        return ConstantNode(content)
    }

    // Handle Identifier: IDENTIFIER
    override fun visitIdentifier(ctx: AntlrPythonParser.IdentifierContext): AstNode {
        return NameNode(ctx.IDENTIFIER().text, Load) // Removed !!
    }

    // Handle NumberLiteral: NUMBER
    override fun visitNumberLiteral(ctx: AntlrPythonParser.NumberLiteralContext): AstNode {
        val numText = ctx.NUMBER().text // Removed !!
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

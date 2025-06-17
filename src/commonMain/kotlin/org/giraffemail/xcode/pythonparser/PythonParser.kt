package org.giraffemail.xcode.pythonparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.common.ParserUtils
import org.giraffemail.xcode.generated.PythonLexer
import org.giraffemail.xcode.generated.PythonParser as AntlrPythonParser
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.tree.ParseTreeVisitor
import org.giraffemail.xcode.generated.PythonBaseVisitor
import org.giraffemail.xcode.parserbase.AbstractAntlrParser

object PythonParser : AbstractAntlrParser<PythonLexer, AntlrPythonParser, AntlrPythonParser.ProgramContext>() {

    private val indentationHandler = PythonIndentationHandler()

    override fun preprocessCode(code: String): String {
        // First extract metadata comments and store them for processing
        val codeWithoutMetadata = extractMetadataFromCode(code)
        // Then apply indentation processing
        return indentationHandler.processIndentation(codeWithoutMetadata)
    }

    override fun createLexer(charStream: CharStream): PythonLexer {
        return PythonLexer(charStream)
    }

    override fun createAntlrParser(tokens: CommonTokenStream): AntlrPythonParser {
        return AntlrPythonParser(tokens)
    }

    override fun invokeEntryPoint(parser: AntlrPythonParser): AntlrPythonParser.ProgramContext {
        return parser.program()
    }

    override fun createAstBuilder(): ParseTreeVisitor<AstNode> {
        return PythonAstBuilder()
    }

    override fun getLanguageName(): String {
        return "Python"
    }

    override fun postprocessAst(ast: AstNode): AstNode {
        return injectMetadataIntoAst(ast)
    }
    
    private val metadataQueue = mutableListOf<LanguageMetadata>()
    
    private fun extractMetadataFromCode(code: String): String {
        metadataQueue.clear()
        val lines = code.split('\n')
        val cleanedLines = mutableListOf<String>()
        
        for (line in lines) {
            if (line.contains("__META__:")) {
                // Extract metadata and add to queue
                MetadataSerializer.extractMetadataFromComment(line)?.let { metadata ->
                    metadataQueue.add(metadata)
                }
                // Remove the metadata comment line from code to be parsed
                val cleanedLine = line.replace(Regex("#.*__META__:.*"), "").trim()
                if (cleanedLine.isNotEmpty()) {
                    cleanedLines.add(cleanedLine)
                }
            } else {
                cleanedLines.add(line)
            }
        }
        
        return cleanedLines.joinToString("\n")
    }
    
    private fun injectMetadataIntoAst(ast: AstNode): AstNode {
        // Instead of using index, match metadata by type to appropriate nodes
        val functionMetadata = metadataQueue.filter { it.returnType != null || it.paramTypes.isNotEmpty() }
        val assignmentMetadata = metadataQueue.filter { it.variableType != null }
        
        var functionMetadataIndex = 0
        var assignmentMetadataIndex = 0
        
        fun injectIntoNode(node: AstNode): AstNode {
            return when (node) {
                is ModuleNode -> {
                    val processedBody = node.body.map { stmt ->
                        injectIntoNode(stmt) as StatementNode
                    }
                    node.copy(body = processedBody)
                }
                is FunctionDefNode -> {
                    if (functionMetadataIndex < functionMetadata.size) {
                        val metadata = functionMetadata[functionMetadataIndex++]
                        val metadataMap = mutableMapOf<String, Any>()
                        if (metadata.returnType != null) {
                            metadataMap["returnType"] = metadata.returnType
                        }
                        if (metadata.paramTypes.isNotEmpty()) {
                            metadataMap["paramTypes"] = metadata.paramTypes
                        }
                        
                        // Restore individual parameter metadata
                        val updatedArgs = node.args.map { param ->
                            val paramMetadata = metadata.individualParamMetadata[param.id]
                            if (paramMetadata != null && paramMetadata.isNotEmpty()) {
                                param.copy(metadata = paramMetadata)
                            } else {
                                param
                            }
                        }
                        
                        // Process function body recursively
                        val updatedBody = node.body.map { stmt ->
                            injectIntoNode(stmt) as StatementNode
                        }
                        
                        node.copy(
                            args = updatedArgs,
                            body = updatedBody,
                            metadata = metadataMap.ifEmpty { null }
                        )
                    } else {
                        // No function metadata, but still need to process body
                        val updatedBody = node.body.map { stmt ->
                            injectIntoNode(stmt) as StatementNode
                        }
                        node.copy(body = updatedBody)
                    }
                }
                is AssignNode -> {
                    if (assignmentMetadataIndex < assignmentMetadata.size) {
                        val metadata = assignmentMetadata[assignmentMetadataIndex++]
                        if (metadata.variableType != null) {
                            node.copy(metadata = mapOf("variableType" to metadata.variableType))
                        } else {
                            node
                        }
                    } else {
                        node
                    }
                }
                else -> node
            }
        }
        
        return injectIntoNode(ast)
    }

    // The main parse method is now inherited from AbstractAntlrParser.
    // The original parse method's content, including the "trigger_error" check,
    // try-catch block, and preprocessing, is now handled by the abstract class
    // and the overrides above.

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

    // Handle Comparison expression
    override fun visitComparison(ctx: AntlrPythonParser.ComparisonContext): AstNode {
        val leftOperandCtx = ctx.expression(0)
        val rightOperandCtx = ctx.expression(1)

        // Use the shared utility method to extract comparison operator
        val operator = ParserUtils.extractComparisonOperator(ctx.text)

        // Ensure operands are not null before visiting
        if (leftOperandCtx == null || rightOperandCtx == null) {
            println("Error in Python visitComparison: one or both operands are null for input '${ctx.text}'")
            val errorLeft = UnknownNode("Missing left operand in comparison: ${leftOperandCtx?.text ?: "null"}")
            val errorRight = UnknownNode("Missing right operand in comparison: ${rightOperandCtx?.text ?: "null"}")
            return CompareNode(errorLeft, operator, errorRight)
        }

        val visitedLeft = visit(leftOperandCtx)
        val visitedRight = visit(rightOperandCtx)

        if (visitedLeft is ExpressionNode && visitedRight is ExpressionNode) {
            return CompareNode(visitedLeft, operator, visitedRight)
        } else {
            println("Error in Python visitComparison: operands are not valid ExpressionNodes. Left: $visitedLeft, Right: $visitedRight for input '${ctx.text}'")
            val errorLeft = visitedLeft as? ExpressionNode ?: UnknownNode("Invalid left operand in comparison: ${leftOperandCtx.text}")
            val errorRight = visitedRight as? ExpressionNode ?: UnknownNode("Invalid right operand in comparison: ${rightOperandCtx.text}")
            return CompareNode(errorLeft, operator, errorRight)
        }
    }

    override fun visitFunctionDef(ctx: AntlrPythonParser.FunctionDefContext): AstNode {
        val name = ctx.IDENTIFIER().text // Removed !! as IDENTIFIER is not optional in grammar

        // Parse parameters
        val parameters = mutableListOf<NameNode>()
        ctx.parameters()?.parameter()?.forEach { paramCtx ->
            val paramId = paramCtx.IDENTIFIER().text // Removed !!
            parameters.add(NameNode(id = paramId, ctx = Param))
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
        val callNode = createCallNode(funcName, ctx.arguments())
        return CallStatementNode(call = callNode)
    }

    // Handle if statements
    override fun visitIfStatement(ctx: AntlrPythonParser.IfStatementContext): AstNode {
        val condition = visit(ctx.expression()) as? ExpressionNode
            ?: UnknownNode("Invalid condition in if statement")

        // Get the if body (first function_body)
        val ifBody = ctx.function_body(0)?.let { visit(it) as? ModuleNode }?.body ?: emptyList()

        // Get the else body if present (second function_body)
        val elseBody = if (ctx.function_body().size > 1) {
            ctx.function_body(1)?.let { visit(it) as? ModuleNode }?.body ?: emptyList()
        } else {
            emptyList()
        }

        return IfNode(test = condition, body = ifBody, orelse = elseBody)
    }

    // Handle function calls in expressions - UPDATED for FunctionCallInExpression label
    override fun visitFunctionCallInExpression(ctx: AntlrPythonParser.FunctionCallInExpressionContext): AstNode {
        val funcName = ctx.IDENTIFIER().text // Removed !!
        return createCallNode(funcName, ctx.arguments())
    }

    private fun createCallNode(funcName: String, argumentsCtx: AntlrPythonParser.ArgumentsContext?): CallNode {
        val funcNameNode = NameNode(id = funcName, ctx = Load)
        val args = mutableListOf<ExpressionNode>()
        argumentsCtx?.expression()?.forEach { exprCtx ->
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

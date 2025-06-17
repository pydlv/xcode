package org.giraffemail.xcode.typescriptparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.common.ParserUtils
import org.giraffemail.xcode.generated.TypeScriptLexer
import org.giraffemail.xcode.generated.TypeScriptParser as AntlrTypeScriptParser
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.tree.ParseTreeVisitor
import org.giraffemail.xcode.generated.TypeScriptBaseVisitor
import org.giraffemail.xcode.parserbase.AbstractAntlrParser

object TypeScriptParser : AbstractAntlrParser<TypeScriptLexer, AntlrTypeScriptParser, AntlrTypeScriptParser.ProgramContext>() {

    override fun createLexer(charStream: CharStream): TypeScriptLexer {
        return TypeScriptLexer(charStream)
    }

    override fun createAntlrParser(tokens: CommonTokenStream): AntlrTypeScriptParser {
        return AntlrTypeScriptParser(tokens)
    }

    override fun invokeEntryPoint(parser: AntlrTypeScriptParser): AntlrTypeScriptParser.ProgramContext {
        return parser.program() // Assuming 'program' is the entry rule in TypeScript.g4
    }

    override fun createAstBuilder(): ParseTreeVisitor<AstNode> {
        return TypeScriptAstBuilder()
    }

    override fun getLanguageName(): String {
        return "TypeScript"
    }

    override fun postprocessAst(ast: AstNode): AstNode {
        return injectMetadataIntoAst(ast)
    }
    
    override fun preprocessCode(code: String): String {
        // No preprocessing needed since we don't support comment-based metadata
        metadataQueue.clear()
        return code
    }
    
    private val metadataQueue = mutableListOf<LanguageMetadata>()
    
    private fun injectMetadataIntoAst(ast: AstNode): AstNode {
        return ParserUtils.injectMetadataIntoAst(ast, metadataQueue)
    }

    /**
     * Parse method that supports parts-based metadata
     */
    fun parseWithMetadata(code: String, metadataPart: List<LanguageMetadata>): AstNode {
        // Specific trigger for testing error handling paths
        if (code == "trigger_error_${getLanguageName().lowercase()}") {
            throw AstParseException("Simulated parsing error for 'trigger_error_${getLanguageName().lowercase()}' input in ${getLanguageName()}.")
        }
        
        // Use parts-based metadata
        val processedCode = ParserUtils.extractMetadataFromPart(code, metadataPart, metadataQueue)
        
        val lexer = createLexer(org.antlr.v4.kotlinruntime.CharStreams.fromString(processedCode))
        val tokens = org.antlr.v4.kotlinruntime.CommonTokenStream(lexer)
        val parser = createAntlrParser(tokens)
        val parseTree = invokeEntryPoint(parser)
        val visitor = createAstBuilder()
        val ast = parseTree.accept(visitor)
        return postprocessAst(ast)
    }

    // The main parse method is now inherited from AbstractAntlrParser.
    // The original parse method's content, including the "trigger_error" check
    // and try-catch block, is now handled by the abstract class and the overrides above.
}

// Visitor to convert ANTLR ParseTree to your custom AST for TypeScript
class TypeScriptAstBuilder : TypeScriptBaseVisitor<AstNode>() {
    override fun visitProgram(ctx: AntlrTypeScriptParser.ProgramContext): AstNode {
        val statements = ctx.statement().mapNotNull { visit(it) as? StatementNode }
        // Assuming ModuleNode is appropriate for a TypeScript program.
        return ModuleNode(statements)
    }

    override fun visitConsoleLogStatement(ctx: AntlrTypeScriptParser.ConsoleLogStatementContext): AstNode {
        val expression = visit(ctx.expression()) as? ExpressionNode
            ?: UnknownNode("Invalid expression in console.log")
        return PrintNode(expression)
    }

    override fun visitFunctionDeclaration(ctx: AntlrTypeScriptParser.FunctionDeclarationContext): AstNode {
        val functionName = ctx.IDENTIFIER().text
        
        // Handle parameters and extract type information
        val params = ctx.parameterList()?.parameter()?.map { paramCtx ->
            val paramName = paramCtx.IDENTIFIER().text
            val paramType = paramCtx.typeAnnotation()?.typeExpression()?.text
            val metadata = if (paramType != null) {
                mapOf("type" to paramType)
            } else null
            NameNode(id = paramName, ctx = Param, metadata = metadata)
        } ?: emptyList()

        // Handle function body
        val body = ctx.functionBody()?.let { visit(it) as? ModuleNode }?.body ?: emptyList()

        // Extract return type annotation if present
        val returnType = ctx.typeAnnotation()?.typeExpression()?.text
        
        val functionMetadata = if (returnType != null || params.any { it.metadata != null }) {
            val metadata = mutableMapOf<String, Any>()
            if (returnType != null) {
                metadata["returnType"] = returnType
            }
            val paramTypes = params.mapNotNull { param ->
                param.metadata?.get("type")?.let { param.id to it }
            }.toMap()
            if (paramTypes.isNotEmpty()) {
                metadata["paramTypes"] = paramTypes
            }
            metadata
        } else null

        return FunctionDefNode(
            name = functionName,
            args = params,
            body = body,
            decoratorList = emptyList(),
            metadata = functionMetadata
        )
    }

    // Handle function body
    override fun visitFunctionBody(ctx: AntlrTypeScriptParser.FunctionBodyContext): AstNode {
        val statements = ctx.statement().mapNotNull { stmtCtx ->
            visit(stmtCtx) as? StatementNode
        }
        return ModuleNode(body = statements)
    }

    // Handle variable assignment statements
    override fun visitAssignStatement(ctx: AntlrTypeScriptParser.AssignStatementContext): AstNode {
        val targetId = ctx.IDENTIFIER().text // Keep null check for safety
        val targetNode = NameNode(id = targetId, ctx = Store)

        val valueExpr = visit(ctx.expression()) as? ExpressionNode
            ?: UnknownNode("Invalid expression in assignment")

        // Extract type annotation if present
        val variableType = ctx.typeAnnotation()?.typeExpression()?.text
        
        val assignmentMetadata = if (variableType != null) {
            mapOf("variableType" to variableType)
        } else null

        return AssignNode(target = targetNode, value = valueExpr, metadata = assignmentMetadata)
    }

    // Handle function calls as statements
    override fun visitFunctionCallStatement(ctx: AntlrTypeScriptParser.FunctionCallStatementContext): AstNode {
        val funcName = ctx.IDENTIFIER().text // Keep null check for safety
        val callNode = createCallNode(funcName, ctx.arguments())
        return CallStatementNode(call = callNode)
    }

    // Handle function calls in expressions
    override fun visitFunctionCall(ctx: AntlrTypeScriptParser.FunctionCallContext): AstNode {
        val funcName = ctx.IDENTIFIER().text // Keep null check for safety
        return createCallNode(funcName, ctx.arguments())
    }

    // Handle if statements
    override fun visitIfStatement(ctx: AntlrTypeScriptParser.IfStatementContext): AstNode {
        val condition = ParserUtils.visitAsExpressionNode(visit(ctx.expression()), "Invalid condition in if statement")

        // Get the if body (first functionBody)
        val ifBody = ctx.functionBody(0)?.let { visit(it) as? ModuleNode }?.body ?: emptyList()

        // Get the else body if present (second functionBody)
        val elseBody = if (ctx.functionBody().size > 1) {
            ctx.functionBody(1)?.let { visit(it) as? ModuleNode }?.body ?: emptyList()
        } else {
            emptyList()
        }

        return IfNode(
            test = condition,
            body = ifBody,
            orelse = elseBody
        )
    }

    // Expression visitors
    override fun visitAddition(ctx: AntlrTypeScriptParser.AdditionContext): AstNode {
        try {
            val left = visit(ctx.getChild(0)!!) as? ExpressionNode
                ?: UnknownNode("Invalid left expression in addition")

            val right = visit(ctx.getChild(2)!!) as? ExpressionNode
                ?: UnknownNode("Invalid right expression in addition")

            return BinaryOpNode(left, "+", right)
        } catch (e: Exception) {
            println("Error parsing TypeScript addition: ${e.message}")
            return UnknownNode("Error in addition expression")
        }
    }

    override fun visitComparison(ctx: AntlrTypeScriptParser.ComparisonContext): AstNode {
        try {
            val left = ParserUtils.visitAsExpressionNode(visit(ctx.getChild(0)!!), "Invalid left expression in comparison")
            val right = ParserUtils.visitAsExpressionNode(visit(ctx.getChild(2)!!), "Invalid right expression in comparison")
            val rawOperator = ctx.getChild(1)!!.text
            
            return ParserUtils.createComparisonNode(left, rawOperator, right)
        } catch (e: Exception) {
            println("Error parsing TypeScript comparison: ${e.message}")
            return UnknownNode("Error in comparison expression")
        }
    }

    override fun visitStringLiteral(ctx: AntlrTypeScriptParser.StringLiteralContext): AstNode {
        // Remove the quotes from the string literal
        val quotedString = ctx.STRING_LITERAL().text
        val unquotedString = quotedString.substring(1, quotedString.length - 1)
        return ConstantNode(value = unquotedString)
    }

    override fun visitNumberLiteral(ctx: AntlrTypeScriptParser.NumberLiteralContext): AstNode {
        val numText = ctx.NUMBER().text
        // TypeScript numbers are floating-point, but normalize to integers when possible for common AST
        val doubleValue = numText.toDoubleOrNull() ?: 0.0 // Default to 0.0 if parsing fails for some reason
        
        // If the double represents a whole number, use an integer for common AST consistency
        val normalizedValue = if (doubleValue == doubleValue.toInt().toDouble()) {
            doubleValue.toInt()
        } else {
            doubleValue
        }
        
        return ConstantNode(normalizedValue)
    }

    override fun visitIdentifier(ctx: AntlrTypeScriptParser.IdentifierContext): AstNode {
        return NameNode(ctx.IDENTIFIER().text, Load)
    }

    override fun defaultResult(): AstNode {
        return UnknownNode("Unhandled ANTLR TypeScript node")
    }

    // Corrected signature to match the base class for non-nullable generic type T (AstNode)
    override fun aggregateResult(aggregate: AstNode, nextResult: AstNode): AstNode {
        return nextResult
    }

    // Helper method to create call nodes
    private fun createCallNode(funcName: String, argumentsCtx: AntlrTypeScriptParser.ArgumentsContext?): CallNode {
        val funcNameNode = ParserUtils.createFunctionNameNode(funcName)
        val args = mutableListOf<ExpressionNode>()
        argumentsCtx?.expression()?.forEach { exprCtx ->
            val arg = visit(exprCtx) as? ExpressionNode
            if (arg != null) {
                args.add(arg)
            }
        }
        return CallNode(func = funcNameNode, args = args, keywords = emptyList())
    }
}
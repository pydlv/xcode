package org.giraffemail.xcode.javascriptparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.generated.JavaScriptLexer
import org.giraffemail.xcode.generated.JavaScriptParser as AntlrJavaScriptParser
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.tree.ParseTreeVisitor
import org.giraffemail.xcode.generated.JavaScriptBaseVisitor
import org.giraffemail.xcode.parserbase.AbstractAntlrParser
import org.giraffemail.xcode.common.ParserUtils

object JavaScriptParser : AbstractAntlrParser<JavaScriptLexer, AntlrJavaScriptParser, AntlrJavaScriptParser.ProgramContext>() {

    override fun createLexer(charStream: CharStream): JavaScriptLexer {
        return JavaScriptLexer(charStream)
    }

    override fun createAntlrParser(tokens: CommonTokenStream): AntlrJavaScriptParser {
        return AntlrJavaScriptParser(tokens)
    }

    override fun invokeEntryPoint(parser: AntlrJavaScriptParser): AntlrJavaScriptParser.ProgramContext {
        return parser.program() // Assuming 'program' is the entry rule in JavaScript.g4
    }

    override fun createAstBuilder(): ParseTreeVisitor<AstNode> {
        return JavaScriptAstBuilder()
    }

    override fun getLanguageName(): String {
        return "JavaScript"
    }
    
    override fun postprocessAst(ast: AstNode): AstNode {
        // Base class handles metadata injection in parseWithMetadata
        return ast
    }
    
    override fun preprocessCode(code: String): String {
        // No preprocessing needed since we don't support comment-based metadata
        return code
    }
    
    // The parseWithMetadata method is now inherited from AbstractAntlrParser.
    // The metadata injection and parsing logic is handled by the base class.
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
        val funcName = ctx.IDENTIFIER().text // Keep null check for safety, though grammar might ensure IDENTIFIER exists

        // Parse parameters
        val parameters = mutableListOf<NameNode>()
        ctx.parameterList()?.IDENTIFIER()?.forEach { paramIdent ->
            val paramName = paramIdent.text
            parameters.add(NameNode(id = paramName, ctx = Param))
        }

        // Parse function body statements
        val body = ctx.functionBody().statement().mapNotNull { stmtCtx ->
            visit(stmtCtx) as? StatementNode
        }

        return FunctionDefNode(
            name = funcName,
            args = parameters,
            body = body,
            decoratorList = emptyList()
        )
    }

    override fun visitClassDeclaration(ctx: AntlrJavaScriptParser.ClassDeclarationContext): AstNode {
        val name = ctx.IDENTIFIER(0)?.text ?: "UnknownClass" // First IDENTIFIER is the class name
        
        // Parse base class for inheritance (extends)
        val baseClasses = mutableListOf<ExpressionNode>()
        if (ctx.IDENTIFIER().size > 1) {
            // Second IDENTIFIER is the base class
            val baseClassName = ctx.IDENTIFIER(1)?.text
            if (baseClassName != null) {
                baseClasses.add(NameNode(id = baseClassName, ctx = Load))
            }
        }

        // Parse class body - process class members
        val body = ctx.classBody().classMember().mapNotNull { visit(it) as? StatementNode }

        return ClassDefNode(
            name = name,
            baseClasses = baseClasses,
            body = body,
            decoratorList = emptyList()
        )
    }

    override fun visitClassBody(ctx: AntlrJavaScriptParser.ClassBodyContext): AstNode {
        val statements = ctx.classMember().mapNotNull { visit(it) as? StatementNode }
        return ModuleNode(statements) // Using ModuleNode to represent a block of statements
    }

    override fun visitClassMember(ctx: AntlrJavaScriptParser.ClassMemberContext): AstNode {
        return when {
            ctx.functionDeclaration() != null -> ctx.functionDeclaration()?.let { visit(it) } ?: UnknownNode("Null function declaration")
            ctx.assignStatement() != null -> ctx.assignStatement()?.let { visit(it) } ?: UnknownNode("Null assign statement")
            else -> UnknownNode("Unknown class member: ${ctx.text}")
        }
    }

    // Handle function body
    override fun visitFunctionBody(ctx: AntlrJavaScriptParser.FunctionBodyContext): AstNode {
        val statements = ctx.statement().mapNotNull { stmtCtx ->
            visit(stmtCtx) as? StatementNode
        }
        return ModuleNode(body = statements)
    }

    // Handle variable assignment statements
    override fun visitAssignStatement(ctx: AntlrJavaScriptParser.AssignStatementContext): AstNode {
        val targetId = ctx.IDENTIFIER().text // Keep null check for safety
        val targetNode = NameNode(id = targetId, ctx = Store)

        val valueExpr = visit(ctx.expression()) as? ExpressionNode
            ?: UnknownNode("Invalid expression in assignment")

        return AssignNode(target = targetNode, value = valueExpr)
    }

    // Handle function calls as statements
    override fun visitFunctionCallStatement(ctx: AntlrJavaScriptParser.FunctionCallStatementContext): AstNode {
        val funcName = ctx.IDENTIFIER().text // Keep null check for safety
        val callNode = createCallNode(funcName, ctx.arguments())
        return CallStatementNode(call = callNode)
    }

    // Handle function calls in expressions
    override fun visitFunctionCall(ctx: AntlrJavaScriptParser.FunctionCallContext): AstNode {
        val funcName = ctx.IDENTIFIER().text // Keep null check for safety
        return createCallNode(funcName, ctx.arguments())
    }

    // Handle if statements
    override fun visitIfStatement(ctx: AntlrJavaScriptParser.IfStatementContext): AstNode {
        val condition = ParserUtils.visitAsExpressionNode(visit(ctx.expression()), "Invalid condition in if statement")

        // Get the if body (first functionBody)
        val ifBody = ctx.functionBody(0)?.let { visit(it) as? ModuleNode }?.body ?: emptyList()

        // Get the else body if present (second functionBody)
        val elseBody = if (ctx.functionBody().size > 1) {
            ctx.functionBody(1)?.let { visit(it) as? ModuleNode }?.body ?: emptyList()
        } else {
            emptyList()
        }

        return IfNode(test = condition, body = ifBody, orelse = elseBody)
    }

    // Handle return statements
    override fun visitReturnStatement(ctx: AntlrJavaScriptParser.ReturnStatementContext): AstNode {
        val returnValue = ctx.expression()?.let { exprCtx ->
            visit(exprCtx) as? ExpressionNode
        }
        return ReturnNode(value = returnValue)
    }

    private fun createCallNode(funcName: String, argumentsCtx: AntlrJavaScriptParser.ArgumentsContext?): CallNode {
        val funcNameNode = ParserUtils.createFunctionNameNode(funcName)
        val args = mutableListOf<ExpressionNode>()
        argumentsCtx?.expression()?.forEach { exprCtx -> // argumentsCtx can be null, and expression() can be null within argumentsCtx
            (exprCtx as? AntlrJavaScriptParser.ExpressionContext)?.let {
                val arg = visit(it) as? ExpressionNode
                if (arg != null) {
                    args.add(arg)
                }
            }
        }
        return CallNode(func = funcNameNode, args = args)
    }

    // Handle Addition expression
    override fun visitAddition(ctx: AntlrJavaScriptParser.AdditionContext): AstNode {
        try {
            val left = visit(ctx.getChild(0)!!) as? ExpressionNode
                ?: UnknownNode("Invalid left expression in addition")

            val right = visit(ctx.getChild(2)!!) as? ExpressionNode
                ?: UnknownNode("Invalid right expression in addition")

            return BinaryOpNode(left, "+", right)
        } catch (e: Exception) {
            println("Error parsing JavaScript addition: ${e.message}")
            return UnknownNode("Error in addition expression")
        }
    }

    // Handle Comparison expression
    override fun visitComparison(ctx: AntlrJavaScriptParser.ComparisonContext): AstNode {
        try {
            val left = ParserUtils.visitAsExpressionNode(visit(ctx.getChild(0)!!), "Invalid left expression in comparison")
            val right = ParserUtils.visitAsExpressionNode(visit(ctx.getChild(2)!!), "Invalid right expression in comparison")
            val rawOperator = ctx.getChild(1)!!.text
            
            return ParserUtils.createComparisonNode(left, rawOperator, right)
        } catch (e: Exception) {
            println("Error parsing JavaScript comparison: ${e.message}")
            return UnknownNode("Error in comparison expression")
        }
    }

    // Handle StringLiteral: STRING_LITERAL
    override fun visitStringLiteral(ctx: AntlrJavaScriptParser.StringLiteralContext): AstNode {
        val text = ctx.STRING_LITERAL().text // Removed !! as it's not nullable
        val content = if (text.length >= 2) text.substring(1, text.length - 1) else ""
        return ConstantNode(content)
    }

    // Handle Identifier: IDENTIFIER
    override fun visitIdentifier(ctx: AntlrJavaScriptParser.IdentifierContext): AstNode {
        return NameNode(ctx.IDENTIFIER().text, Load) // Removed !! as it's not nullable
    }

    // Handle NumberLiteral: NUMBER
    override fun visitNumberLiteral(ctx: AntlrJavaScriptParser.NumberLiteralContext): AstNode {
        val numText = ctx.NUMBER().text // Removed !! as it's not nullable
        // JavaScript numbers are floating-point, but normalize to integers when possible for common AST
        val doubleValue = numText.toDoubleOrNull() ?: 0.0 // Default to 0.0 if parsing fails for some reason
        
        // If the double represents a whole number, use an integer for common AST consistency
        val normalizedValue = if (doubleValue == doubleValue.toInt().toDouble()) {
            doubleValue.toInt()
        } else {
            doubleValue
        }
        
        return ConstantNode(normalizedValue)
    }

    override fun defaultResult(): AstNode {
        return UnknownNode("Unhandled ANTLR JavaScript node")
    }

    // Corrected signature to match the base class for non-nullable generic type T (AstNode)
    override fun aggregateResult(aggregate: AstNode, nextResult: AstNode): AstNode {
        return nextResult
    }
}

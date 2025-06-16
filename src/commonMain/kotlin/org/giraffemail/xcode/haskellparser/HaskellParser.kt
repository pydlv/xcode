package org.giraffemail.xcode.haskellparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.generated.HaskellLexer
import org.giraffemail.xcode.generated.HaskellParser as AntlrHaskellParser
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.tree.ParseTreeVisitor
import org.giraffemail.xcode.generated.HaskellBaseVisitor
import org.giraffemail.xcode.parserbase.AbstractAntlrParser

object HaskellParser : AbstractAntlrParser<HaskellLexer, AntlrHaskellParser, AntlrHaskellParser.ProgramContext>() {

    override fun createLexer(charStream: CharStream): HaskellLexer {
        return HaskellLexer(charStream)
    }

    override fun createAntlrParser(tokens: CommonTokenStream): AntlrHaskellParser {
        return AntlrHaskellParser(tokens)
    }

    override fun invokeEntryPoint(parser: AntlrHaskellParser): AntlrHaskellParser.ProgramContext {
        return parser.program()
    }

    override fun createAstBuilder(): ParseTreeVisitor<AstNode> {
        return HaskellAstBuilder()
    }

    override fun getLanguageName(): String {
        return "Haskell"
    }
}

class HaskellAstBuilder : HaskellBaseVisitor<AstNode>() {
    
    override fun defaultResult(): AstNode {
        return UnknownNode("Default node")
    }
    
    override fun visitProgram(ctx: AntlrHaskellParser.ProgramContext): AstNode {
        val statements = ctx.topLevelDeclaration().mapNotNull { visit(it) as? StatementNode }
        return ModuleNode(statements)
    }

    override fun visitTopLevelDeclaration(ctx: AntlrHaskellParser.TopLevelDeclarationContext): AstNode {
        return ctx.functionDefinition()?.let { visit(it) } ?: ctx.statement()?.let { visit(it) } ?: defaultResult()
    }

    override fun visitStatement(ctx: AntlrHaskellParser.StatementContext): AstNode {
        return ctx.printStatement()?.let { visit(it) }
            ?: ctx.assignment()?.let { visit(it) }
            ?: ctx.functionCallStatement()?.let { visit(it) }
            ?: ctx.ifStatement()?.let { visit(it) }
            ?: defaultResult()
    }

    override fun visitPrintStatement(ctx: AntlrHaskellParser.PrintStatementContext): AstNode {
        val expression = visit(ctx.expression()) as? ExpressionNode
            ?: UnknownNode("Invalid expression in print statement: ${ctx.expression().text}")
        return PrintNode(expression)
    }

    override fun visitFunctionDefinition(ctx: AntlrHaskellParser.FunctionDefinitionContext): AstNode {
        val name = ctx.IDENTIFIER().text
        val params = ctx.parameterList()?.IDENTIFIER()?.map { 
            NameNode(id = it.text, ctx = Param) 
        } ?: emptyList()
        
        // In Haskell, function body is an expression, but we need to convert to statements
        val bodyExpr = visit(ctx.expression()) as? ExpressionNode
        val body = if (bodyExpr != null) {
            // Convert expression to a statement (e.g., return statement or expression statement)
            listOf(ExpressionStatementNode(bodyExpr))
        } else {
            emptyList()
        }
        
        return FunctionDefNode(
            name = name,
            args = params,
            body = body,
            decorator_list = emptyList()
        )
    }

    override fun visitAssignment(ctx: AntlrHaskellParser.AssignmentContext): AstNode {
        val target = NameNode(id = ctx.IDENTIFIER().text, ctx = Store)
        val value = visit(ctx.expression()) as? ExpressionNode
            ?: UnknownNode("Invalid expression in assignment: ${ctx.expression().text}")
        return AssignNode(target = target, value = value)
    }

    override fun visitFunctionCallStatement(ctx: AntlrHaskellParser.FunctionCallStatementContext): AstNode {
        val funcName = NameNode(id = ctx.IDENTIFIER().text, ctx = Load)
        val args = ctx.arguments()?.expression()?.mapNotNull { visit(it) as? ExpressionNode } ?: emptyList()
        val call = CallNode(func = funcName, args = args, keywords = emptyList())
        return CallStatementNode(call = call)
    }

    override fun visitIfStatement(ctx: AntlrHaskellParser.IfStatementContext): AstNode {
        val expressions = ctx.expression()
        val test = if (expressions.isNotEmpty()) {
            visit(expressions[0]) as? ExpressionNode ?: UnknownNode("Invalid test expression in if statement")
        } else {
            UnknownNode("Missing test expression in if statement")
        }
        
        val thenExpr = if (expressions.size > 1) {
            visit(expressions[1]) as? ExpressionNode
        } else null
        
        val elseExpr = if (expressions.size > 2) {
            visit(expressions[2]) as? ExpressionNode
        } else null
        
        // Convert expressions to statements
        val thenStmt = if (thenExpr != null) listOf(ExpressionStatementNode(thenExpr)) else emptyList()
        val elseStmt = if (elseExpr != null) listOf(ExpressionStatementNode(elseExpr)) else emptyList()
        
        return IfNode(test = test, body = thenStmt, orelse = elseStmt)
    }

    override fun visitAddition(ctx: AntlrHaskellParser.AdditionContext): AstNode {
        val expressions = ctx.expression()
        val left = if (expressions.isNotEmpty()) {
            visit(expressions[0]) as? ExpressionNode ?: UnknownNode("Invalid left operand in addition")
        } else {
            UnknownNode("Missing left operand in addition")
        }
        val right = if (expressions.size > 1) {
            visit(expressions[1]) as? ExpressionNode ?: UnknownNode("Invalid right operand in addition")
        } else {
            UnknownNode("Missing right operand in addition")
        }
        return BinaryOpNode(left = left, op = "+", right = right)
    }

    override fun visitComparison(ctx: AntlrHaskellParser.ComparisonContext): AstNode {
        val expressions = ctx.expression()
        val left = if (expressions.isNotEmpty()) {
            visit(expressions[0]) as? ExpressionNode ?: UnknownNode("Invalid left operand in comparison")
        } else {
            UnknownNode("Missing left operand in comparison")
        }
        val right = if (expressions.size > 1) {
            visit(expressions[1]) as? ExpressionNode ?: UnknownNode("Invalid right operand in comparison")
        } else {
            UnknownNode("Missing right operand in comparison")
        }
        
        // Map Haskell operators to common operators  
        val op = when (ctx.getChild(1)?.text) {
            "/=" -> "!="
            else -> ctx.getChild(1)?.text ?: "=="
        }
        
        return CompareNode(left = left, op = op, right = right)
    }

    override fun visitFunctionCall(ctx: AntlrHaskellParser.FunctionCallContext): AstNode {
        val funcName = NameNode(id = ctx.IDENTIFIER().text, ctx = Load)
        val args = ctx.arguments()?.expression()?.mapNotNull { visit(it) as? ExpressionNode } ?: emptyList()
        return CallNode(func = funcName, args = args, keywords = emptyList())
    }

    override fun visitStringLiteral(ctx: AntlrHaskellParser.StringLiteralContext): AstNode {
        val text = ctx.STRING_LITERAL().text
        // Remove surrounding quotes
        val unquoted = text.substring(1, text.length - 1)
        return ConstantNode(unquoted)
    }

    override fun visitIdentifier(ctx: AntlrHaskellParser.IdentifierContext): AstNode {
        return NameNode(id = ctx.IDENTIFIER().text, ctx = Load)
    }

    override fun visitNumberLiteral(ctx: AntlrHaskellParser.NumberLiteralContext): AstNode {
        val text = ctx.NUMBER().text
        return if (text.contains('.')) {
            ConstantNode(text.toDouble())
        } else {
            ConstantNode(text.toInt())
        }
    }

    override fun visitParenthesizedExpression(ctx: AntlrHaskellParser.ParenthesizedExpressionContext): AstNode {
        return visit(ctx.expression())
    }
}
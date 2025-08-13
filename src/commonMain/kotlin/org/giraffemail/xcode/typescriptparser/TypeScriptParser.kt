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
            val canonicalType = if (paramType != null) {
                CanonicalTypes.fromString(paramType)
            } else {
                CanonicalTypes.Unknown
            }
            NameNode(id = paramName, ctx = Param, typeInfo = canonicalType)
        } ?: emptyList()

        // Handle function body
        val body = ctx.functionBody()?.let { visit(it) as? ModuleNode }?.body ?: emptyList()

        // Extract return type annotation if present
        val returnType = ctx.typeAnnotation()?.typeExpression()?.text
        val canonicalReturnType = if (returnType != null) {
            CanonicalTypes.fromString(returnType)
        } else {
            CanonicalTypes.Void
        }
        
        // Extract parameter types
        val paramTypes = params.associate { param ->
            param.id to param.typeInfo
        }.filterValues { 
            when (it) {
                is CanonicalTypes -> it != CanonicalTypes.Unknown
                is TypeDefinition -> true
            }
        }

        return FunctionDefNode(
            name = functionName,
            args = params,
            body = body,
            decoratorList = emptyList(),
            returnType = canonicalReturnType,
            paramTypes = paramTypes
        )
    }

    override fun visitClassDeclaration(ctx: AntlrTypeScriptParser.ClassDeclarationContext): AstNode {
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

    override fun visitClassBody(ctx: AntlrTypeScriptParser.ClassBodyContext): AstNode {
        val statements = ctx.classMember().mapNotNull { visit(it) as? StatementNode }
        return ModuleNode(statements) // Using ModuleNode to represent a block of statements
    }

    override fun visitClassMember(ctx: AntlrTypeScriptParser.ClassMemberContext): AstNode {
        return when {
            ctx.functionDeclaration() != null -> ctx.functionDeclaration()?.let { visit(it) } ?: UnknownNode("Null function declaration")
            ctx.assignStatement() != null -> ctx.assignStatement()?.let { visit(it) } ?: UnknownNode("Null assign statement")
            else -> UnknownNode("Unknown class member: ${ctx.text}")
        }
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
        val variableType = extractTypeExpression(ctx.typeAnnotation()?.typeExpression())
        
        // Check if the value is a list and handle accordingly
        val finalValue = when {
            variableType != null && variableType.startsWith("[") && variableType.contains(",") && valueExpr is ListNode -> {
                // This is a tuple - convert ListNode to TupleNode
                // Parse the tuple type string into individual types
                val typeContent = variableType.substring(1, variableType.length - 1).trim()
                val individualTypes = typeContent.split(",").map { it.trim() }
                val canonicalTypes = individualTypes.map { CanonicalTypes.fromString(it) }
                TupleNode(elements = valueExpr.elements, typeInfo = TypeDefinition.Tuple(canonicalTypes))
            }
            variableType != null && variableType.endsWith("[]") && valueExpr is ListNode -> {
                // This is an array with type information
                val elementType = variableType.substring(0, variableType.length - 2)
                val canonicalElementType = CanonicalTypes.fromString(elementType)
                ListNode(elements = valueExpr.elements, typeInfo = canonicalElementType)
            }
            else -> valueExpr
        }
        
        val canonicalVariableType = if (variableType != null) {
            CanonicalTypes.fromString(variableType)
        } else {
            // Infer typeInfo from the value expression when no type annotation is present
            when (finalValue) {
                is ConstantNode -> finalValue.typeInfo
                is ListNode -> finalValue.typeInfo
                is TupleNode -> finalValue.typeInfo
                is BinaryOpNode -> finalValue.typeInfo
                else -> CanonicalTypes.Unknown
            }
        }

        return AssignNode(target = targetNode, value = finalValue, typeInfo = canonicalVariableType)
    }
    
    // Helper method to extract type expression including array and tuple types
    private fun extractTypeExpression(ctx: AntlrTypeScriptParser.TypeExpressionContext?): String? {
        if (ctx == null) return null
        
        // Get the full text of the type expression
        val text = ctx.text
        
        // Clean up the text representation if needed
        return text
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

    // Handle return statements
    override fun visitReturnStatement(ctx: AntlrTypeScriptParser.ReturnStatementContext): AstNode {
        val returnValue = ctx.expression()?.let { exprCtx ->
            visit(exprCtx) as? ExpressionNode
        }
        return ReturnNode(value = returnValue)
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
        return ConstantNode(value = unquotedString, typeInfo = CanonicalTypes.String)
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
        
        return ConstantNode(normalizedValue, CanonicalTypes.Number)
    }

    override fun visitIdentifier(ctx: AntlrTypeScriptParser.IdentifierContext): AstNode {
        return NameNode(ctx.IDENTIFIER().text, Load)
    }

    override fun visitArrayLiteral(ctx: AntlrTypeScriptParser.ArrayLiteralContext): AstNode {
        val elements = ctx.arrayElements()?.expression()?.mapNotNull { exprCtx ->
            visit(exprCtx) as? ExpressionNode
        } ?: emptyList()
        
        // Infer element type from the parsed elements' typeInfo
        val elementType = when {
            elements.isEmpty() -> CanonicalTypes.Unknown
            elements.all { it is ConstantNode && it.typeInfo == CanonicalTypes.String } -> CanonicalTypes.String
            elements.all { it is ConstantNode && it.typeInfo == CanonicalTypes.Number } -> CanonicalTypes.Number
            elements.all { it is ConstantNode && it.typeInfo == CanonicalTypes.Boolean } -> CanonicalTypes.Boolean
            else -> CanonicalTypes.Unknown
        }
        
        return ListNode(elements = elements, typeInfo = elementType)
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
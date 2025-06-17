package org.giraffemail.xcode.javaparser

import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.tree.ParseTreeVisitor
import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.common.ParserUtils
import org.giraffemail.xcode.generated.JavaLexer
import org.giraffemail.xcode.generated.JavaParser as AntlrJavaParser // Aliased to avoid conflict
import org.giraffemail.xcode.generated.JavaBaseVisitor
import org.giraffemail.xcode.parserbase.AbstractAntlrParser

object JavaParser : AbstractAntlrParser<JavaLexer, AntlrJavaParser, AntlrJavaParser.CompilationUnitContext>() {

    override fun createLexer(charStream: CharStream): JavaLexer {
        return JavaLexer(charStream)
    }

    override fun createAntlrParser(tokens: CommonTokenStream): AntlrJavaParser {
        return AntlrJavaParser(tokens)
    }

    override fun invokeEntryPoint(parser: AntlrJavaParser): AntlrJavaParser.CompilationUnitContext {
        // Consider adding a custom error listener for more detailed error reports
        // parser.removeErrorListeners()
        // parser.addErrorListener(MyCustomErrorListener())
        return parser.compilationUnit() // Entry rule
    }

    override fun createAstBuilder(): ParseTreeVisitor<AstNode> {
        return JavaAstBuilderVisitor()
    }

    override fun getLanguageName(): String {
        return "Java"
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
    
    /**
     * Parse method that supports parts-based metadata
     */
    fun parseWithMetadata(code: String, metadataPart: List<LanguageMetadata>): AstNode {
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
    
    private fun injectMetadataIntoAst(ast: AstNode): AstNode {
        return ParserUtils.injectMetadataIntoAst(ast, metadataQueue)
    }

    // The main parse method is now inherited from AbstractAntlrParser.
    // The original parse method's content is now handled by the abstract class
    // and the overrides above.
}

private class JavaAstBuilderVisitor : JavaBaseVisitor<AstNode>() {

    override fun visitCompilationUnit(ctx: AntlrJavaParser.CompilationUnitContext): ModuleNode {
        val statements = ctx.statement().mapNotNull {
            // It's generally safer to attempt a cast and handle null if a statement might not produce a StatementNode,
            // or if a statement type is unhandled and visitStatement returns null.
            it.accept(this) as? StatementNode
        }
        return ModuleNode(body = statements)
    }

    // visitStatement must return AstNode as per JavaBaseVisitor
    override fun visitStatement(ctx: AntlrJavaParser.StatementContext): AstNode {
        return when {
            ctx.expressionStatement() != null -> ctx.expressionStatement()!!.accept(this) // Added !! based on grammar structure
            ctx.functionDefinition() != null -> ctx.functionDefinition()!!.accept(this)   // Added !!
            ctx.assignmentStatement() != null -> ctx.assignmentStatement()!!.accept(this) // Added !!
            ctx.callStatement() != null -> ctx.callStatement()!!.accept(this)           // Added !!
            ctx.ifStatement() != null -> ctx.ifStatement()!!.accept(this)               // Added !! for if statements
            else -> {
                // This case should ideally not be reached if the grammar is complete for 'statement' alternatives
                // and all alternatives are handled above.
                // Returning an UnknownNode or throwing an exception is better than returning null if AstNode is expected.
                println("Warning: Unhandled or null statement type in visitStatement: ${ctx.text}")
                UnknownNode("Unhandled statement: ${ctx.text}") // Ensure UnknownNode is a valid AstNode
            }
        }
    }

    override fun visitFunctionDefinition(ctx: AntlrJavaParser.FunctionDefinitionContext): FunctionDefNode {
        val name = ctx.IDENTIFIER()!!.text // Added !! assuming IDENTIFIER is mandatory
        val params: List<NameNode> = ctx.parameterList()?.let { paramListCtx: AntlrJavaParser.ParameterListContext ->
            getParameters(paramListCtx)
        } ?: emptyList()
        val body = ctx.statement().mapNotNull { it.accept(this) as? StatementNode } // Process body statements
        return FunctionDefNode(name = name, args = params, body = body, decoratorList = emptyList())
    }

    // visitParameterList should return a List<NameNode>, but the base visitor might expect AstNode.
    // If JavaBaseVisitor expects AstNode for all visit methods, this needs adjustment.
    // For now, assuming this specific signature is intended for internal use or the base visitor is flexible.
    // However, the error indicates it expects AstNode. This method might not be an override of a base visit method then,
    // or it needs to wrap its result in a generic AstNode if it is.
    // Let's assume it's a helper and not an override for now, if it's not in JavaBaseVisitor.
    // If it IS in JavaBaseVisitor, the design is tricky. Let's assume it's a helper.
    // Re-evaluating: The error message implies it *is* an override. This is a conflict.
    // The base ANTLR visitor generator will create visit methods for all labeled rules if not default.
    // If parameterList is a rule, it will have a visitParameterList. This needs to return AstNode.
    // The current AST design doesn't have a node type for "a list of parameters".
    // This indicates a mismatch between grammar structure intended for direct AST mapping and the visitor overrides.

    // Let's assume `parameterList` is a rule and `visitParameterList` is an override.
    // We cannot directly return List<NameNode>. We'd need a wrapper or change the AST.
    // For now, to resolve the compile error, we'll make it return a dummy/placeholder AstNode
    // and the actual parameter list processing will be done by calling a separate helper.
    // This is a workaround for the type system; the real fix might involve AST/grammar redesign.

    fun getParameters(ctx: AntlrJavaParser.ParameterListContext): List<NameNode> {
        return ctx.parameter()?.map { getParameter(it!!) } ?: emptyList()
    }

    fun getParameter(ctx: AntlrJavaParser.ParameterContext): NameNode {
        val paramName = ctx.IDENTIFIER(1)!!.text // Second IDENTIFIER is the name
        return NameNode(id = paramName, ctx = Param)
    }

    // This would be the overridden method, now returning a placeholder
    override fun visitParameterList(ctx: AntlrJavaParser.ParameterListContext): AstNode {
        // This method must return AstNode. The actual list is obtained via getParameters.
        // This is a common pattern if the AST doesn't have a direct node for this list structure.
        // Returning a generic node or even a specific placeholder if useful.
        return UnknownNode("ParameterList placeholder; use getParameters for actual list")
    }

    // visitParameter is likely also an override and must return AstNode.
    override fun visitParameter(ctx: AntlrJavaParser.ParameterContext): AstNode {
        val paramName = ctx.IDENTIFIER(1)!!.text
        return NameNode(id = paramName, ctx = Param)
    }

    // In visitFunctionDefinition, change to use getParameters:
    // val params = ctx.parameterList()?.let { getParameters(it) } ?: emptyList()
    // This was already correct. The issue is the return type of the overridden visitParameterList.


    override fun visitAssignmentStatement(ctx: AntlrJavaParser.AssignmentStatementContext): AssignNode {
        val target = NameNode(id = ctx.IDENTIFIER().text, ctx = Store)
        val value = ctx.expression().accept(this) as? ExpressionNode
            ?: throw IllegalStateException("Assignment value is null or not an ExpressionNode for: ${ctx.text}")
        return AssignNode(target = target, value = value)
    }

    override fun visitCallStatement(ctx: AntlrJavaParser.CallStatementContext): CallStatementNode {
        val call = visitCallExpression(ctx.IDENTIFIER().text, ctx.argumentList())
        return CallStatementNode(call = call)
    }

    override fun visitIfStatement(ctx: AntlrJavaParser.IfStatementContext): IfNode {
        val condition = ctx.expression().accept(this) as? ExpressionNode
            ?: throw IllegalStateException("If condition is null or not an ExpressionNode for: ${ctx.text}")

        // Get the if body (first set of statements)
        val ifBody = ctx.statement().take(ctx.statement().size / (if (ctx.ELSE() != null) 2 else 1))
            .mapNotNull { it.accept(this) as? StatementNode }

        // Get the else body if present
        val elseBody = if (ctx.ELSE() != null) {
            ctx.statement().drop(ctx.statement().size / 2)
                .mapNotNull { it.accept(this) as? StatementNode }
        } else {
            emptyList()
        }

        return IfNode(test = condition, body = ifBody, orelse = elseBody)
    }

    // Helper for CallExpression and CallStatement to build CallNode
    private fun visitCallExpression(functionName: String, argsCtx: AntlrJavaParser.ArgumentListContext?): CallNode {
        val args = argsCtx?.expression()?.mapNotNull {
            it.accept(this) as? ExpressionNode
        } ?: emptyList()
        return CallNode(func = NameNode(id = functionName, ctx = Load), args = args, keywords = emptyList())
    }

    // This handles calls that are part of an expression, if the grammar distinguishes them.
    // The current grammar has IDENTIFIER LPAREN argumentList RPAREN as an 'expression' alternative.
    override fun visitCallExpression(ctx: AntlrJavaParser.CallExpressionContext): CallNode {
        val functionName = ctx.IDENTIFIER().text
        return visitCallExpression(functionName, ctx.argumentList())
    }

    override fun visitExpressionStatement(ctx: AntlrJavaParser.ExpressionStatementContext): AstNode { // Changed JavaParser to AntlrJavaParser
        // An expression statement's AST node is its expression's AST node.
        // Must return non-null AstNode.
        return ctx.expression().accept(this) // Removed unnecessary Elvis operator
    }

    override fun visitPrintlnExpression(ctx: AntlrJavaParser.PrintlnExpressionContext): PrintNode { // Changed JavaParser to AntlrJavaParser
        val expression = ctx.expression().accept(this) as? ExpressionNode // Removed unnecessary safe call
            ?: throw IllegalStateException("Println expression content is null or not an ExpressionNode for: ${ctx.text}")
        return PrintNode(expression = expression)
    }

    override fun visitAdditiveExpression(ctx: AntlrJavaParser.AdditiveExpressionContext): BinaryOpNode { // Changed JavaParser to AntlrJavaParser
        val left = ctx.expression(0)?.accept(this) as? ExpressionNode
            ?: throw IllegalStateException("Left operand of additive expression is null or not an ExpressionNode for: ${ctx.expression(0)?.text}")
        val right = ctx.expression(1)?.accept(this) as? ExpressionNode
            ?: throw IllegalStateException("Right operand of additive expression is null or not an ExpressionNode for: ${ctx.expression(1)?.text}")

        // Access the ADD token via the generated ADD() method and get its symbol
        val opToken = ctx.ADD().symbol // Removed unnecessary safe call and Elvis operator

        val op = when (opToken.type) {
            JavaLexer.Tokens.ADD -> "+"
            // Add other operators here if grammar expands (e.g., SUBTRACT, MULTIPLY)
            else -> throw IllegalArgumentException("Unknown operator type '${opToken.text}' in AdditiveExpression for: ${ctx.text}")
        }
        return BinaryOpNode(left = left, op = op, right = right)
    }

    override fun visitComparisonExpression(ctx: AntlrJavaParser.ComparisonExpressionContext): CompareNode {
        val left = ctx.expression(0)?.accept(this) as? ExpressionNode
            ?: throw IllegalStateException("Left operand of comparison expression is null or not an ExpressionNode for: ${ctx.expression(0)?.text}")
        val right = ctx.expression(1)?.accept(this) as? ExpressionNode
            ?: throw IllegalStateException("Right operand of comparison expression is null or not an ExpressionNode for: ${ctx.expression(1)?.text}")

        // Use the shared utility method to extract comparison operator
        val operator = ParserUtils.extractComparisonOperator(ctx.text)

        return CompareNode(left = left, op = operator, right = right)
    }

    // Handles the 'PrimaryExpression' labeled alternative in the 'expression' rule
    override fun visitPrimaryExpression(ctx: AntlrJavaParser.PrimaryExpressionContext): ExpressionNode { // Changed JavaParser to AntlrJavaParser
        // Delegates to the 'primary' rule's visitor
        // The accept call on primary() should return ExpressionNode or compatible.
        return ctx.primary().accept(this) as? ExpressionNode
            ?: throw IllegalStateException("Primary expression content is null or not an ExpressionNode for: ${ctx.text}")
    }

    // Added for IDENTIFIER as a primary expression (e.g. variable access)
    override fun visitIdentifierPrimary(ctx: AntlrJavaParser.IdentifierPrimaryContext): NameNode {
        return NameNode(id = ctx.IDENTIFIER().text, ctx = Load)
    }

    // Added for IDENTIFIER used in an expression context directly (e.g. as a standalone variable)
    override fun visitIdentifierAccessExpression(ctx: AntlrJavaParser.IdentifierAccessExpressionContext): NameNode {
        return NameNode(id = ctx.IDENTIFIER().text, ctx = Load)
    }

    // Handles the 'LiteralExpression' labeled alternative in the 'primary' rule
    override fun visitLiteralExpression(ctx: AntlrJavaParser.LiteralExpressionContext): ConstantNode {
        // This method corresponds to the # LiteralExpression label in the grammar.
        // It should delegate to the existing visitLiteral method.
        // The LiteralExpressionContext is essentially a wrapper around LiteralContext here.
        // Access the actual LiteralContext using ctx.literal()
        return visitLiteral(ctx.literal())
    }

    override fun visitLiteral(ctx: AntlrJavaParser.LiteralContext): ConstantNode {
        when {
            ctx.STRING_LITERAL() != null -> {
                var text = ctx.STRING_LITERAL()!!.text // Includes outer quotes e.g., "\\\"cookies\\\"" or "'cookies'"

                if (text.length >= 2) {
                    val firstChar = text.first()
                    val lastChar = text.last()
                    // Strip outer quotes if they are a matching pair of single or double quotes
                    // Corrected char literal for single quote to '\''
                    if ((firstChar == '"' && lastChar == '"') || (firstChar == '\'' && lastChar == '\'')) {
                        text = text.substring(1, text.length - 1)
                    }
                }

                // Unescape sequences.
                text = text.replace("\\\\\\\\\\\\\\\\", "\\\\\\\\")  // find \\\\\\\\, replace with \\\\
                text = text.replace("\\\\\\\\\\\\\\\"", "\\\"")  // find \\\\\\\", replace with "
                text = text.replace("\\\\\\\\\\\\\\\'", "\'")    // find \\\\\\', replace with '

                return ConstantNode(text) // Explicit return
            }
            ctx.NUMBER() != null -> { // Changed from DECIMAL_LITERAL to NUMBER
                val textVal = ctx.NUMBER()!!.text // Changed from DECIMAL_LITERAL to NUMBER
                return try {
                    // Java typically treats whole numbers as int
                    ConstantNode(textVal.toInt()) // Explicit return
                } catch (_: NumberFormatException) { // Changed 'e' to '_'
                    try {
                        ConstantNode(textVal.toDouble()) // Explicit return
                    } catch (_: NumberFormatException) { // Changed 'e2' to '_'
                        throw IllegalArgumentException("Could not parse number literal: '$textVal'") // Updated error message
                    }
                }
            }
            else -> throw IllegalArgumentException("Unknown literal type in LiteralContext: ${ctx.text}")
        }
    }

    // Generic visit method from AbstractParseTreeVisitor.
    // This is called when `someNode.accept(this)` is invoked.
    // It then dispatches to the more specific visit<RuleName> methods.
    override fun visit(tree: org.antlr.v4.kotlinruntime.tree.ParseTree): AstNode {
        // Ensure the return type matches the base class expectation.
        // If the specific visit method for 'tree' (e.g., visitCompilationUnit) returns AstNode,
        // this should correctly propagate it.
        // The base visitor's accept should handle the non-null return.
        return tree.accept(this)
    }

    // Default result if a more specific visit method is not overridden for a particular node type
    // and visitChildren is called on its parent.
    override fun defaultResult(): AstNode {
        // Throwing an exception is safer than returning an arbitrary/dummy node
        // unless a specific "UnknownNode" or "EmptyNode" is part of the AST design
        // and appropriate here.
        throw IllegalStateException("defaultResult called: No specific visitor for a node, and a non-null AstNode is required.")
    }

    // Optional: Override aggregateResult if custom aggregation logic is needed.
    // The default implementation in AbstractParseTreeVisitor is usually sufficient:
    // override fun aggregateResult(aggregate: AstNode?, nextResult: AstNode?): AstNode? = nextResult ?: aggregate
}

package org.giraffemail.xcode.javaparser

import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.tree.ParseTreeVisitor
import org.giraffemail.xcode.ast.*
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

    // The main parse method is now inherited from AbstractAntlrParser.
    // The original parse method's content is now handled by the abstract class
    // and the overrides above.
}

private class JavaAstBuilderVisitor : JavaBaseVisitor<AstNode>() {

    override fun visitCompilationUnit(ctx: AntlrJavaParser.CompilationUnitContext): ModuleNode { // Changed JavaParser to AntlrJavaParser
        val statements = ctx.statement().mapNotNull {
            // Ensure that the result of accept is treated as AstNode,
            // then safely cast to StatementNode or filter out if not applicable.
            // If visitStatement now must return AstNode, null check might be less critical here
            // but as? StatementNode handles type safety.
            it.accept(this) as? StatementNode
        }
        return ModuleNode(body = statements)
    }

    // Handles the 'statement' rule from the grammar
    override fun visitStatement(ctx: AntlrJavaParser.StatementContext): AstNode { // Changed JavaParser to AntlrJavaParser
        // Grammar: statement: expressionStatement;
        // Delegate to the expressionStatement visitor
        // Must return non-null AstNode. If expressionStatement can be null in a valid statement,
        // this needs a more robust way to return a valid AstNode (e.g., an EmptyStatementNode).
        return ctx.expressionStatement().accept(this) // Removed unnecessary Elvis operator
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

    // Handles the 'PrimaryExpression' labeled alternative in the 'expression' rule
    override fun visitPrimaryExpression(ctx: AntlrJavaParser.PrimaryExpressionContext): ExpressionNode { // Changed JavaParser to AntlrJavaParser
        // Delegates to the 'primary' rule's visitor
        // The accept call on primary() should return ExpressionNode or compatible.
        return ctx.primary().accept(this) as? ExpressionNode
            ?: throw IllegalStateException("Primary expression content is null or not an ExpressionNode for: ${ctx.text}")
    }

    // Handles the 'LiteralExpression' labeled alternative in the 'primary' rule
    override fun visitLiteralExpression(ctx: AntlrJavaParser.LiteralExpressionContext): ConstantNode {
        // This method corresponds to the # LiteralExpression label in the grammar.
        // It should delegate to the existing visitLiteral method.
        // The LiteralExpressionContext is essentially a wrapper around LiteralContext here.
        // Access the actual LiteralContext using ctx.literal()
        return visitLiteral(ctx.literal())
    }

    // Removed visitPrimary method as it was causing "overrides nothing" error.
    // The logic for primary expressions is handled by visitPrimaryExpression
    // and the specific visitLiteral or other primary alternatives.

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
                try {
                    // Java typically treats whole numbers as int
                    return ConstantNode(textVal.toInt()) // Explicit return
                } catch (_: NumberFormatException) { // Changed 'e' to '_'
                    try {
                        return ConstantNode(textVal.toDouble()) // Explicit return
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

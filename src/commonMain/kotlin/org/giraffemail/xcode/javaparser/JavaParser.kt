package org.giraffemail.xcode.javaparser

import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.generated.JavaLexer
import org.giraffemail.xcode.generated.JavaParser // ANTLR generated parser
import org.giraffemail.xcode.generated.JavaBaseVisitor // ANTLR generated base visitor

object JavaParser {
    fun parse(code: String): AstNode {
        // println("JavaParser.parse called with ANTLR for: $code") // Uncomment for debugging
        val lexer = JavaLexer(CharStreams.fromString(code))
        val tokens = CommonTokenStream(lexer)
        val parser = JavaParser(tokens)
        // Consider adding a custom error listener for more detailed error reports
        // parser.removeErrorListeners()
        // parser.addErrorListener(MyCustomErrorListener())

        val tree = parser.compilationUnit() // Entry rule
        val astBuilder = JavaAstBuilderVisitor()
        // The result of astBuilder.visit(compilationUnitContext) should be ModuleNode (non-null)
        return astBuilder.visit(tree) // Removed unnecessary Elvis operator
    }
}

private class JavaAstBuilderVisitor : JavaBaseVisitor<AstNode>() {

    override fun visitCompilationUnit(ctx: JavaParser.CompilationUnitContext): ModuleNode {
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
    override fun visitStatement(ctx: JavaParser.StatementContext): AstNode {
        // Grammar: statement: expressionStatement;
        // Delegate to the expressionStatement visitor
        // Must return non-null AstNode. If expressionStatement can be null in a valid statement,
        // this needs a more robust way to return a valid AstNode (e.g., an EmptyStatementNode).
        return ctx.expressionStatement().accept(this) // Removed unnecessary Elvis operator
    }

    override fun visitExpressionStatement(ctx: JavaParser.ExpressionStatementContext): AstNode {
        // An expression statement's AST node is its expression's AST node.
        // Must return non-null AstNode.
        return ctx.expression().accept(this) // Removed unnecessary Elvis operator
    }

    override fun visitPrintlnExpression(ctx: JavaParser.PrintlnExpressionContext): PrintNode {
        val expression = ctx.expression().accept(this) as? ExpressionNode // Removed unnecessary safe call
            ?: throw IllegalStateException("Println expression content is null or not an ExpressionNode for: ${ctx.text}")
        return PrintNode(expression = expression)
    }

    override fun visitAdditiveExpression(ctx: JavaParser.AdditiveExpressionContext): BinaryOpNode {
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
    override fun visitPrimaryExpression(ctx: JavaParser.PrimaryExpressionContext): ExpressionNode {
        // Delegates to the 'primary' rule's visitor
        // The accept call on primary() should return ExpressionNode or compatible.
        return ctx.primary().accept(this) as? ExpressionNode // Removed unnecessary safe call
            ?: throw IllegalStateException("Primary expression content is null or not an ExpressionNode for: ${ctx.text}")
    }

    // Handles the 'LiteralExpression' labeled alternative in the 'primary' rule
    override fun visitLiteralExpression(ctx: JavaParser.LiteralExpressionContext): ConstantNode {
        // This method corresponds to the # LiteralExpression label in the grammar.
        // It should delegate to the existing visitLiteral method.
        // The LiteralExpressionContext is essentially a wrapper around LiteralContext here.
        // Access the actual LiteralContext using ctx.literal()
        return visitLiteral(ctx.literal())
    }

    // Removed visitPrimary method as it was causing "overrides nothing" error.
    // The logic for primary expressions is handled by visitPrimaryExpression
    // and the specific visitLiteral or other primary alternatives.

    override fun visitLiteral(ctx: JavaParser.LiteralContext): ConstantNode {
        return when {
            ctx.STRING_LITERAL() != null -> {
                var text = ctx.STRING_LITERAL()!!.text
                // Remove surrounding quotes
                if (text.length >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
                    text = text.substring(1, text.length - 1)
                }
                // Unescape internal escaped quotes (e.g., \\\" -> \")
                text = text.replace("\\\"\"", "\"")
                ConstantNode(text)
            }
            ctx.DECIMAL_LITERAL() != null -> {
                val text = ctx.DECIMAL_LITERAL()!!.text
                try {
                    // Java typically treats whole numbers as int
                    ConstantNode(text.toInt())
                } catch (_: NumberFormatException) { // Changed 'e' to '_'
                    try {
                        ConstantNode(text.toDouble())
                    } catch (_: NumberFormatException) { // Changed 'e2' to '_'
                        throw IllegalArgumentException("Could not parse decimal literal: '$text'")
                    }
                }
            }
            else -> throw IllegalArgumentException("Unknown literal type in LiteralContext: ${ctx.text}")
        }
    }

    // Generic visit method from AbstractParseTreeVisitor.
    // This is called when `someNode.accept(this)` is invoked.
    // It then dispatches to the more specific visit<RuleName> methods.
    override fun visit(tree: org.antlr.v4.kotlinruntime.tree.ParseTree): AstNode { // Return type changed to non-nullable AstNode
        return tree.accept(this) // Removed unnecessary Elvis operator
    }

    // Default result if a more specific visit method is not overridden for a particular node type
    // and visitChildren is called on its parent.
    override fun defaultResult(): AstNode {
        // If the base class (ultimately AbstractParseTreeVisitor via JavaBaseVisitor<AstNode>)
        // insists on a non-null AstNode, we cannot return null.
        // Throwing an exception is safer than returning an arbitrary/dummy node
        // unless a specific "UnknownNode" or "EmptyNode" is part of the AST design
        // and appropriate here.
        throw IllegalStateException("defaultResult called: No specific visitor for a node, and a non-null AstNode is required.")
    }

    // Optional: Override aggregateResult if custom aggregation logic is needed.
    // The default implementation in AbstractParseTreeVisitor is usually sufficient:
    // override fun aggregateResult(aggregate: AstNode?, nextResult: AstNode?): AstNode? = nextResult ?: aggregate
}

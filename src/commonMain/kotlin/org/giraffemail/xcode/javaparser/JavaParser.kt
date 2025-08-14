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
            ctx.classDefinition() != null -> ctx.classDefinition()!!.accept(this)         // Added !! for class definitions
            ctx.assignmentStatement() != null -> ctx.assignmentStatement()!!.accept(this) // Added !!
            ctx.callStatement() != null -> ctx.callStatement()!!.accept(this)           // Added !!
            ctx.ifStatement() != null -> ctx.ifStatement()!!.accept(this)               // Added !! for if statements
            ctx.forStatement() != null -> ctx.forStatement()!!.accept(this)             // Added !! for for statements
            ctx.returnStatement() != null -> ctx.returnStatement()!!.accept(this)       // Added !! for return statements
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
        val body = ctx.statementBlock()?.statement()?.mapNotNull { it.accept(this) as? StatementNode } ?: emptyList() // Process body statements from statementBlock
        return FunctionDefNode(name = name, args = params, body = body, decoratorList = emptyList())
    }

    override fun visitClassDefinition(ctx: AntlrJavaParser.ClassDefinitionContext): ClassDefNode {
        val name = ctx.IDENTIFIER(0)!!.text // First IDENTIFIER is the class name
        
        // Parse base class for inheritance (extends)
        val baseClasses = mutableListOf<ExpressionNode>()
        if (ctx.IDENTIFIER().size > 1) {
            // Second IDENTIFIER is the base class
            baseClasses.add(NameNode(id = ctx.IDENTIFIER(1)!!.text, ctx = Load))
        }

        // Parse class body - process statements from statementBlock
        val body = ctx.statementBlock()?.statement()?.mapNotNull { visit(it) as? StatementNode } ?: emptyList()

        return ClassDefNode(
            name = name,
            baseClasses = baseClasses,
            body = body,
            decoratorList = emptyList()
        )
    }

    // Add visitor for statementBlock
    override fun visitStatementBlock(ctx: AntlrJavaParser.StatementBlockContext): AstNode {
        // For statement blocks, we typically want to return the list of statements
        // But since this must return AstNode, we'll return a ModuleNode containing the statements
        val statements = ctx.statement()?.mapNotNull { visit(it) as? StatementNode } ?: emptyList()
        return ModuleNode(body = statements)
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
        val paramName = ctx.IDENTIFIER().text // Single IDENTIFIER is the name
        val paramType = ctx.type()?.accept(this) as? String ?: "Object" // Extract type from type rule
        
        // Convert to CanonicalTypes
        val canonicalType = if (paramType.lowercase() != "object") {
            CanonicalTypes.fromString(paramType)
        } else {
            CanonicalTypes.Unknown // Changed from Any to Unknown for round-trip consistency
        }
        
        return NameNode(id = paramName, ctx = Param, typeInfo = canonicalType)
    }

    override fun visitType(ctx: AntlrJavaParser.TypeContext): AstNode {
        // Build the type string including array brackets
        val baseType = if (ctx.IDENTIFIER() != null) {
            ctx.IDENTIFIER()!!.text
        } else if (ctx.primitiveType() != null) {
            ctx.primitiveType()!!.accept(this) as? String ?: "Object"
        } else {
            "Object"
        }
        
        // Count the number of array brackets
        val arrayDimensions = ctx.LBRACKET().size
        val typeString = if (arrayDimensions > 0) {
            baseType + "[]".repeat(arrayDimensions)
        } else {
            baseType
        }
        
        // Return the type as a string wrapped in a ConstantNode
        return ConstantNode(typeString)
    }

    override fun visitPrimitiveType(ctx: AntlrJavaParser.PrimitiveTypeContext): AstNode {
        // Extract the primitive type text
        val typeText = ctx.text // This will get 'int', 'double', etc.
        return ConstantNode(typeText)
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
        val paramName = ctx.IDENTIFIER().text // Single IDENTIFIER is the name
        val paramType = ctx.type()?.accept(this) as? String ?: "Object" // Extract type from type rule
        
        // Convert to CanonicalTypes
        val canonicalType = if (paramType.lowercase() != "object") {
            CanonicalTypes.fromString(paramType)
        } else {
            CanonicalTypes.Unknown // Changed from Any to Unknown for round-trip consistency
        }
        
        return NameNode(id = paramName, ctx = Param, typeInfo = canonicalType)
    }

    // In visitFunctionDefinition, change to use getParameters:
    // val params = ctx.parameterList()?.let { getParameters(it) } ?: emptyList()
    // This was already correct. The issue is the return type of the overridden visitParameterList.


    override fun visitAssignmentStatement(ctx: AntlrJavaParser.AssignmentStatementContext): AssignNode {
        // ANTLR's Java.g4 has been updated to support typed declarations
        // The grammar is: (type IDENTIFIER ASSIGN expression | IDENTIFIER ASSIGN expression) SEMI
        
        val targetName: String 
        val variableType: String?
        
        if (ctx.type() != null) {
            // Typed declaration: type IDENTIFIER = expression
            variableType = ctx.type()!!.text.lowercase() // Normalize type to lowercase for consistency
            targetName = ctx.IDENTIFIER()!!.text  // When type is present, there's only one IDENTIFIER
        } else {
            // Simple assignment: IDENTIFIER = expression
            targetName = ctx.IDENTIFIER()!!.text
            variableType = null
        }
        
        val target = NameNode(id = targetName, ctx = Store)
        val value = ctx.expression()!!.accept(this) as? ExpressionNode
            ?: throw IllegalStateException("Assignment value is null or not an ExpressionNode for: ${ctx.text}")
        
        // Convert type to CanonicalTypes
        val canonicalType = if (variableType != null) {
            CanonicalTypes.fromString(variableType)
        } else {
            // Infer typeInfo from the value expression when no type annotation is present
            when (value) {
                is ConstantNode -> value.typeInfo
                is ListNode -> value.typeInfo
                is TupleNode -> value.typeInfo
                is BinaryOpNode -> value.typeInfo
                else -> CanonicalTypes.Unknown
            }
        }
        
        return AssignNode(target = target, value = value, typeInfo = canonicalType)
    }

    override fun visitCallStatement(ctx: AntlrJavaParser.CallStatementContext): CallStatementNode {
        val call = visitCallExpression(ctx.IDENTIFIER().text, ctx.argumentList())
        return CallStatementNode(call = call)
    }

    override fun visitIfStatement(ctx: AntlrJavaParser.IfStatementContext): IfNode {
        val condition = ctx.expression().accept(this) as? ExpressionNode
            ?: throw IllegalStateException("If condition is null or not an ExpressionNode for: ${ctx.text}")

        // Get the if body from the first statementBlock
        val ifBodyStatements = ctx.statementBlock(0)?.statement()?.mapNotNull { it.accept(this) as? StatementNode } ?: emptyList()

        // Get the else body if present (second statementBlock)
        val elseBodyStatements = if (ctx.ELSE() != null && ctx.statementBlock().size > 1) {
            ctx.statementBlock(1)?.statement()?.mapNotNull { it.accept(this) as? StatementNode } ?: emptyList()
        } else {
            emptyList()
        }

        return IfNode(test = condition, body = ifBodyStatements, orelse = elseBodyStatements)
    }

    override fun visitForStatement(ctx: AntlrJavaParser.ForStatementContext): CStyleForLoopNode {
        // Extract init, condition, and update from the C-style for loop
        val init = ctx.forInit()?.accept(this)?.let { node ->
            if (node is UnknownNode && node.description.contains("empty")) null
            else node as? StatementNode
        }
        val condition = ctx.expression()?.accept(this) as? ExpressionNode
        val update = ctx.forUpdate()?.accept(this)?.let { node ->
            if (node is UnknownNode && node.description.contains("empty")) null
            else node as? ExpressionNode
        }

        // Get the for body (statements from statementBlock)
        val forBody = ctx.statementBlock()?.statement()?.mapNotNull { it.accept(this) as? StatementNode } ?: emptyList()

        return CStyleForLoopNode(init = init, condition = condition, update = update, body = forBody)
    }

    override fun visitForInit(ctx: AntlrJavaParser.ForInitContext): AstNode {
        // Check if we have a typed declaration (type IDENTIFIER = expression)
        return if (ctx.type() != null && ctx.IDENTIFIER() != null && ctx.ASSIGN() != null) {
            // This is a variable declaration with initialization
            val type = ctx.type()!!.accept(this) as? String ?: "Object"
            val varName = ctx.IDENTIFIER()!!.text
            val value = ctx.expression()?.accept(this) as? ExpressionNode 
                ?: ConstantNode(0) // Default value if expression is missing
            
            val typeInfo = CanonicalTypes.fromString(type)
            val target = NameNode(id = varName, ctx = Store, typeInfo = typeInfo)
            AssignNode(target = target, value = value, typeInfo = typeInfo)
        } else if (ctx.IDENTIFIER() != null && ctx.ASSIGN() != null) {
            // Simple assignment (IDENTIFIER = expression)
            val varName = ctx.IDENTIFIER()!!.text
            val value = ctx.expression()?.accept(this) as? ExpressionNode 
                ?: ConstantNode(0)
            
            val target = NameNode(id = varName, ctx = Store)
            AssignNode(target = target, value = value)
        } else {
            // Empty init - return a constant to satisfy non-null requirement
            UnknownNode("empty for init")
        }
    }

    override fun visitForUpdate(ctx: AntlrJavaParser.ForUpdateContext): AstNode {
        return if (ctx.IDENTIFIER() != null) {
            val varName = ctx.IDENTIFIER()!!.text
            val operand = NameNode(id = varName, ctx = Load)
            
            when {
                ctx.INCR() != null -> UnaryOpNode(operand = operand, op = "++", prefix = false)
                ctx.DECR() != null -> UnaryOpNode(operand = operand, op = "--", prefix = false)
                ctx.assignmentExpression() != null -> {
                    // Handle assignment expression in update
                    ctx.assignmentExpression()!!.accept(this) as? ExpressionNode ?: UnknownNode("invalid assignment expression")
                }
                else -> UnknownNode("empty for update")
            }
        } else {
            UnknownNode("empty for update")
        }
    }

    override fun visitAssignmentExpression(ctx: AntlrJavaParser.AssignmentExpressionContext): AstNode {
        val varName = ctx.IDENTIFIER().text
        val value = ctx.expression().accept(this) as? ExpressionNode 
            ?: ConstantNode(0)
        
        // For assignment expressions in for updates, we need to return a BinaryOpNode representing the assignment
        // This is a bit unusual but necessary for the AST structure
        val target = NameNode(id = varName, ctx = Load)
        return BinaryOpNode(left = target, op = "=", right = value)
    }

    // Handle return statements
    override fun visitReturnStatement(ctx: AntlrJavaParser.ReturnStatementContext): ReturnNode {
        val returnValue = ctx.expression()?.let { exprCtx ->
            exprCtx.accept(this) as? ExpressionNode
        }
        return ReturnNode(value = returnValue)
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

                return ConstantNode(text, CanonicalTypes.String) // Explicit return
            }
            ctx.NUMBER() != null -> { // Changed from DECIMAL_LITERAL to NUMBER
                val textVal = ctx.NUMBER()!!.text // Changed from DECIMAL_LITERAL to NUMBER
                return try {
                    // Java typically treats whole numbers as int
                    ConstantNode(textVal.toInt(), CanonicalTypes.Number) // Explicit return
                } catch (_: NumberFormatException) { // Changed 'e' to '_'
                    try {
                        ConstantNode(textVal.toDouble(), CanonicalTypes.Number) // Explicit return
                    } catch (_: NumberFormatException) { // Changed 'e2' to '_'
                        throw IllegalArgumentException("Could not parse number literal: '$textVal'") // Updated error message
                    }
                }
            }
            ctx.BOOLEAN_LITERAL() != null -> {
                val boolValue = ctx.BOOLEAN_LITERAL()!!.text == "true"
                return ConstantNode(boolValue, CanonicalTypes.Boolean)
            }
            else -> throw IllegalArgumentException("Unknown literal type in LiteralContext: ${ctx.text}")
        }
    }
    
    // Handle array initializer expressions
    override fun visitArrayInitializerExpression(ctx: AntlrJavaParser.ArrayInitializerExpressionContext): AstNode {
        return visit(ctx.arrayInitializer())
    }
    
    override fun visitArrayInit(ctx: AntlrJavaParser.ArrayInitContext): AstNode {
        val elements = ctx.arrayElements()?.expression()?.mapNotNull { exprCtx ->
            visit(exprCtx) as? ExpressionNode
        } ?: emptyList()
        
        // Extract array type if available and normalize to lowercase for consistency
        val arrayType = ctx.type()?.text?.replace("[]", "")?.lowercase() // Remove [] from type and normalize
        val canonicalArrayType = if (arrayType != null) {
            CanonicalTypes.fromString(arrayType)
        } else {
            CanonicalTypes.Unknown
        }
        
        return ListNode(elements = elements, typeInfo = canonicalArrayType)
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

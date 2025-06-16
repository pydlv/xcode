package org.giraffemail.xcode.parserbase

import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.CharStreams // Added import
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Lexer
import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.tree.ParseTree
import org.antlr.v4.kotlinruntime.tree.ParseTreeVisitor
import org.giraffemail.xcode.ast.AstNode
import org.giraffemail.xcode.ast.AstParseException

abstract class AbstractAntlrParser<
    L : Lexer,
    AntlrP : Parser, // Renamed to avoid conflict with kotlin.Parser
    PTN : ParseTree // Type of the entry point parse tree node
> {

    protected abstract fun createLexer(charStream: CharStream): L
    protected abstract fun createAntlrParser(tokens: CommonTokenStream): AntlrP
    protected abstract fun invokeEntryPoint(parser: AntlrP): PTN
    protected abstract fun createAstBuilder(): ParseTreeVisitor<AstNode> // Visitor that produces our AstNode
    protected abstract fun getLanguageName(): String // For logging and error messages

    protected open fun preprocessCode(code: String): String {
        return code // Default implementation does no preprocessing
    }

    fun parse(code: String): AstNode {
        // println("${getLanguageName()}Parser.parse attempting to parse with ANTLR: '$code'") // Verbose logging

        // Specific trigger for testing error handling paths
        if (code == "trigger_error_${getLanguageName().lowercase()}") { // Changed to lowercase()
            throw AstParseException("Simulated parsing error for 'trigger_error_${getLanguageName().lowercase()}' input in ${getLanguageName()}.") // Changed to lowercase()
        }

        try {
            val processedCode = preprocessCode(code)
            // Conditional logging for Python's preprocessed code, as it's significant
            if (getLanguageName() == "Python" && code != processedCode) { // Log only if changed
                 println("Preprocessed ${getLanguageName()} code (first 100 chars):\n${processedCode.take(100)}${if (processedCode.length > 100) "..." else ""}")
            }

            val lexer = createLexer(CharStreams.fromString(processedCode))
            // Custom error listeners can be attached here if needed:
            // lexer.removeErrorListeners()
            // lexer.addErrorListener(MyCustomLexerErrorListener())

            val tokens = CommonTokenStream(lexer)
            val antlrParser = createAntlrParser(tokens)
            // parser.removeErrorListeners()
            // parser.addErrorListener(MyCustomParserErrorListener())

            val tree = invokeEntryPoint(antlrParser)
            val astBuilder = createAstBuilder()

            // The AstBuilder's visit method is expected to return a non-null AstNode.
            // If it could return null, the AstBuilder or its base visitor types would need to allow nullable AstNode.
            return astBuilder.visit(tree)

        } catch (e: AstParseException) {
            // Re-throw AstParseException as it's our defined exception type for parsing issues
            throw e
        } catch (e: Exception) {
            // Wrap other unexpected exceptions
            // Consider logging the stack trace for unexpected errors during development/debugging
            // e.printStackTrace()
            println("ERROR: ANTLR parsing failed for ${getLanguageName()} with unexpected exception: ${e.message}") // Changed to println
            throw AstParseException("Failed to parse ${getLanguageName()} code using ANTLR: ${e.message}", e)
        }
    }
}

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
    
    protected open fun postprocessAst(ast: AstNode): AstNode {
        return ast // Default implementation does no post-processing
    }
}

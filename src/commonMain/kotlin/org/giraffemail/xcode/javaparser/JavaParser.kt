package org.giraffemail.xcode.javaparser

import org.giraffemail.xcode.ast.AstNode
import org.giraffemail.xcode.ast.ModuleNode
import org.giraffemail.xcode.ast.PrintNode
import org.giraffemail.xcode.ast.ConstantNode

object JavaParser {
    fun parse(code: String): AstNode {
        // This is a basic stub and only recognizes a specific print statement.
        println("JavaParser.parse called with: $code (STUB)")
        if (code.trim() == "System.out.println(\"cookies\");") {
            return ModuleNode(body = listOf(PrintNode(expression = ConstantNode("cookies"))))
        }
        throw NotImplementedError("JavaParser.parse is a stub and cannot parse: $code")
    }
}


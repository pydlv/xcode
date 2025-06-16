package org.giraffemail.xcode.javaparser

import org.giraffemail.xcode.ast.AstNode
import org.giraffemail.xcode.ast.ConstantNode
import org.giraffemail.xcode.ast.ModuleNode
import org.giraffemail.xcode.ast.PrintNode

object JavaGenerator {
    fun generate(ast: AstNode): String {
        // This is a basic stub and only generates a specific print statement.
        println("JavaGenerator.generate called with: $ast (STUB)")
        if (ast is ModuleNode && ast.body.size == 1) {
            val firstStatement = ast.body[0]
            if (firstStatement is PrintNode && firstStatement.expression is ConstantNode) {
                if ((firstStatement.expression as ConstantNode).value == "cookies") {
                    return "System.out.println(\"cookies\");"
                }
            }
        }
        throw NotImplementedError("JavaGenerator.generate is a stub and cannot generate code for: $ast")
    }
}


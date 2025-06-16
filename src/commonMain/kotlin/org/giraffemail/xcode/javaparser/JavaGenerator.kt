package org.giraffemail.xcode.javaparser

import org.giraffemail.xcode.ast.*

object JavaGenerator {
    fun generate(ast: AstNode): String {
        println("JavaGenerator.generate called with ANTLR-compatible logic for: $ast")
        return when (ast) {
            is ModuleNode -> {
                ast.body.joinToString(separator = "\n") { generate(it) }
            }
            is PrintNode -> {
                val expressionStr = generate(ast.expression)
                "System.out.println($expressionStr);"
            }
            is BinaryOpNode -> {
                val leftStr = generate(ast.left)
                val rightStr = generate(ast.right)
                // Basic operator mapping, can be extended
                val opStr = when (ast.op) {
                    "+" -> "+"
                    // Add other operators as needed (e.g., "-", "*", "/")
                    else -> throw NotImplementedError("Operator ${ast.op} not implemented in JavaGenerator")
                }
                "$leftStr $opStr $rightStr"
            }
            is ConstantNode -> {
                when (val value = ast.value) {
                    is String -> "\"${value.replace("\"", "\\\"")}\"" // Escape quotes in strings
                    is Number -> value.toString() // Handles Int, Double, etc.
                    // Add other constant types as needed (e.g., Boolean)
                    else -> throw NotImplementedError("Constant type ${value?.let { it::class.simpleName }} not implemented in JavaGenerator")
                }
            }
            // Add other AST node types as needed (e.g., FunctionDefNode, CallNode, AssignNode)
            else -> throw NotImplementedError("AST node type ${ast::class.simpleName} not implemented in JavaGenerator")
        }
    }
}

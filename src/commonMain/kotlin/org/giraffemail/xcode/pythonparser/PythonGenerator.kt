package org.giraffemail.xcode.pythonparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.common.AbstractAstGenerator

class PythonGenerator : AbstractAstGenerator() {

    override fun getStatementSeparator(): String = "\n"

    override fun getStatementTerminator(): String = ""

    override fun formatStringLiteral(value: String): String = "'${value.replace("'", "\\'")}'"

    override fun formatFunctionName(name: String): String {
        return when (name) {
            "console.log" -> "print"
            else -> name
        }
    }

    override fun visitPrintNode(node: PrintNode): String {
        // Use formatFunctionName to handle potential mappings, though "print" is standard.
        return "${formatFunctionName("print")}(${generateExpression(node.expression)})"
    }

    override fun visitFunctionDefNode(node: FunctionDefNode): String {
        val funcName = node.name
        val params = node.args.joinToString(", ") { it.id } // Assuming args are NameNodes for params
        // Each statement in the body needs to be indented.
        val body = node.body.joinToString("\n") { "    " + generateStatement(it) }
        
        return "def $funcName($params):\n$body"
    }

    override fun visitClassDefNode(node: ClassDefNode): String {
        // Generate Python class structure
        val className = node.name
        val baseClassDecl = if (node.baseClasses.isNotEmpty()) {
            "(${node.baseClasses.joinToString(", ") { generateExpression(it) }})"
        } else {
            ""
        }
        val classMethods = node.body.joinToString("\n\n") { "    " + generateStatement(it) }
        
        return "class $className$baseClassDecl:\n$classMethods"
    }

    override fun visitAssignNode(node: AssignNode): String {
        // Compiler warning in JSGenerator implied node.target is always NameNode.
        // If AstNode.AssignNode.target is confirmed to be NameNode, this cast is safe.
        // Otherwise, the original if/else structure might be needed if target can be other ExpressionNode types.
        val targetName = node.target.id // Assuming node.target is of type NameNode
        val valueExpr = generateExpression(node.value)
        
        return "$targetName = $valueExpr"
    }

    override fun visitCallStatementNode(node: CallStatementNode): String {
        // In Python, a call statement is just the expression, no semicolon.
        return generateExpression(node.call)
    }

    override fun visitReturnNode(node: ReturnNode): String {
        return if (node.value != null) {
            "return ${generateExpression(node.value)}"
        } else {
            "return"
        }
    }

    override fun visitCallNode(node: CallNode): String {
        val funcString = when (val funcNode = node.func) {
            is NameNode -> formatFunctionName(funcNode.id) // Handles mapping like "console.log"
            is MemberExpressionNode -> {
                // Special handling for console.log from JS AST
                if (funcNode.obj is NameNode && (funcNode.obj as NameNode).id == "console" &&
                    funcNode.property is NameNode && (funcNode.property as NameNode).id == "log") {
                    formatFunctionName("console.log") // This will return "print"
                } else {
                    // For other MemberExpressionNodes, generate them as expressions
                    generateExpression(funcNode) // This will call visitMemberExpressionNode
                }
            }
            else -> generateExpression(funcNode) // Fallback for other func types
        }
        val args = generateArgumentList(node.args)
        return "$funcString($args)"
    }

    override fun visitMemberExpressionNode(node: MemberExpressionNode): String {
        val objStr = generateExpression(node.obj)
        // Revert to checking type of property before accessing .id
        val propStr = if (node.property is NameNode) node.property.id else generateExpression(node.property)
        return "$objStr.$propStr"
    }

    override fun visitConstantNode(node: ConstantNode): String {
        return when (val value = node.value) {
            is Boolean -> if (value) "True" else "False" // Python-specific boolean literals
            // Delegate to super for strings (which handles quoting) and numbers (int/double/float conversion)
            else -> super.visitConstantNode(node)
        }
    }

    override fun visitIfNode(node: IfNode): String {
        val condition = generateExpression(node.test)
        val ifBody = node.body.joinToString("\n") { "    " + generateStatement(it) }
        
        return if (node.orelse.isNotEmpty()) {
            val elseBody = node.orelse.joinToString("\n") { "    " + generateStatement(it) }
            "if $condition:\n$ifBody\nelse:\n$elseBody"
        } else {
            "if $condition:\n$ifBody"
        }
    }

    override fun visitCompareNode(node: CompareNode): String {
        val leftStr = generateExpression(node.left)
        val rightStr = generateExpression(node.right)
        // Convert JavaScript-style operators back to Python-style
        val pythonOp = when (node.op) {
            "===" -> "=="
            "!==" -> "!="
            else -> node.op
        }
        return "$leftStr $pythonOp $rightStr"
    }

    // visitNameNode, visitBinaryOpNode, visitUnknownNode, visitExprNode, visitModuleNode
    // will use the open implementations from AbstractAstGenerator.
}

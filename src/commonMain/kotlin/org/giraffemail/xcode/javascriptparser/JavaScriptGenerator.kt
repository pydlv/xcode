package org.giraffemail.xcode.javascriptparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.common.AbstractAstGenerator

class JavaScriptGenerator : AbstractAstGenerator() {

    override fun getStatementSeparator(): String = "\n"

    override fun getStatementTerminator(): String = ";"

    override fun formatStringLiteral(value: String): String = "'${value.replace("'", "\\'")}'"

    override fun formatFunctionName(name: String): String {
        return when (name) {
            "print" -> "console.log"
            else -> name
        }
    }

    override fun visitPrintNode(node: PrintNode): String {
        return "${formatFunctionName("print")}(${generateExpression(node.expression)})${getStatementTerminator()}"
    }

    override fun visitFunctionDefNode(node: FunctionDefNode): String {
        val funcName = node.name
        val params = node.args.joinToString(", ") { it.id } // Assuming args are NameNodes for params
        // Indent statements within the function body
        val body = node.body.joinToString("\n") { "    " + generateStatement(it) }
        
        return "function $funcName($params) {\n$body\n}"
    }

    override fun visitClassDefNode(node: ClassDefNode): String {
        // Generate JavaScript class structure
        val className = node.name
        val baseClassDecl = if (node.baseClasses.isNotEmpty()) {
            " extends ${node.baseClasses.first().let { generateExpression(it) }}"
        } else {
            ""
        }
        val classMethods = node.body.joinToString("\n\n") { "    " + generateStatement(it) }
        
        return "class $className$baseClassDecl {\n$classMethods\n}"
    }

    override fun visitAssignNode(node: AssignNode): String {
        // Compiler warning indicated 'node.target is NameNode' is always true.
        // This implies node.target is already NameNode or a subtype from which .id can be accessed.
        val targetName = node.target.id // Assuming node.target is of type NameNode
        val valueExpr = generateExpression(node.value)
        
        return "let $targetName = $valueExpr${getStatementTerminator()}"
    }

    override fun visitCallStatementNode(node: CallStatementNode): String {
        return "${generateExpression(node.call)}${getStatementTerminator()}"
    }

    override fun visitReturnNode(node: ReturnNode): String {
        return if (node.value != null) {
            "return ${generateExpression(node.value)}${getStatementTerminator()}"
        } else {
            "return${getStatementTerminator()}"
        }
    }

    override fun visitCallNode(node: CallNode): String {
        val funcString = when (val funcNode = node.func) {
            is NameNode -> formatFunctionName(funcNode.id) // Handles mapping like "print"
            // MemberExpressionNode (e.g., obj.method) is handled by generateExpression calling visitMemberExpressionNode
            else -> generateExpression(funcNode)
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

    override fun visitCompareNode(node: CompareNode): String {
        // Convert == to === for JavaScript strict equality
        val jsOp = when (node.op) {
            "==" -> "==="
            "!=" -> "!=="
            else -> node.op
        }
        return generateBinaryOperation(node.left, jsOp, node.right)
    }

    // visitConstantNode will use the base implementation which calls formatStringLiteral for strings.
    // Numbers and booleans will be formatted by the base class's visitConstantNode.
    // If JS needs specific boolean (true/false) or number formatting different from base, override visitConstantNode.

    // visitNameNode, visitBinaryOpNode, visitUnknownNode, visitExprNode, visitModuleNode
    // will use the open implementations from AbstractAstGenerator.
}

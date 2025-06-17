package org.giraffemail.xcode.typescriptparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.common.AbstractAstGenerator

class TypeScriptGenerator : AbstractAstGenerator() {

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

    override fun visitAssignNode(node: AssignNode): String {
        // Compiler warning indicated 'node.target is NameNode' is always true.
        // This implies node.target is already NameNode or a subtype from which .id can be accessed.
        val targetName = node.target.id // Assuming node.target is of type NameNode
        val valueExpr = generateExpression(node.value)
        // Using 'let' for assignments, could be 'var' or 'const' based on further requirements
        return "let $targetName = $valueExpr${getStatementTerminator()}"
    }

    override fun visitCallStatementNode(node: CallStatementNode): String {
        return "${generateExpression(node.call)}${getStatementTerminator()}"
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

    override fun visitIfNode(node: IfNode): String {
        val condition = generateExpression(node.test)
        val ifBody = node.body.joinToString("\n") { "    " + generateStatement(it) }
        
        return if (node.orelse.isNotEmpty()) {
            val elseBody = node.orelse.joinToString("\n") { "    " + generateStatement(it) }
            "if ($condition) {\n$ifBody\n} else {\n$elseBody\n}"
        } else {
            "if ($condition) {\n$ifBody\n}"
        }
    }

    override fun visitCompareNode(node: CompareNode): String {
        val leftStr = generateExpression(node.left)
        val rightStr = generateExpression(node.right)
        // Convert == to === for TypeScript strict equality
        val tsOp = when (node.op) {
            "==" -> "==="
            "!=" -> "!=="
            else -> node.op
        }
        return "$leftStr $tsOp $rightStr"
    }

    // visitConstantNode will use the base implementation which calls formatStringLiteral for strings.
    // Numbers and booleans will be formatted by the base class's visitConstantNode.
    // If TypeScript needs specific boolean (true/false) or number formatting different from base, override visitConstantNode.

    // visitNameNode, visitBinaryOpNode, visitUnknownNode, visitExprNode, visitModuleNode
    // will use the open implementations from AbstractAstGenerator.
}
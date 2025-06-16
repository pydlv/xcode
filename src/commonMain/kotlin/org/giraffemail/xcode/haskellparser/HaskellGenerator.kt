package org.giraffemail.xcode.haskellparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.common.AbstractAstGenerator

class HaskellGenerator : AbstractAstGenerator() {

    override fun getStatementSeparator(): String = "\n"

    override fun getStatementTerminator(): String = ""

    override fun formatStringLiteral(value: String): String = "\"${value.replace("\"", "\\\"")}\""

    override fun formatFunctionName(name: String): String {
        return when (name) {
            "print" -> "putStrLn"
            "console.log" -> "putStrLn"
            else -> name
        }
    }

    override fun visitPrintNode(node: PrintNode): String {
        val exprStr = generateExpression(node.expression)
        // Add parentheses if the expression is complex (contains operators)
        val needsParens = node.expression is BinaryOpNode || node.expression is CompareNode
        val arg = if (needsParens) "($exprStr)" else exprStr
        return "${formatFunctionName("print")} $arg"
    }

    override fun visitFunctionDefNode(node: FunctionDefNode): String {
        val funcName = node.name
        val params = node.args.joinToString(" ") { it.id }
        
        // In Haskell, function definitions are expressions, not statements
        // For simplicity, we'll convert the body to a single expression
        val body = if (node.body.isEmpty()) {
            "undefined"
        } else {
            // Convert the first statement to an expression
            val firstStmt = node.body.first()
            when (firstStmt) {
                is PrintNode -> "${formatFunctionName("print")} ${generateExpression(firstStmt.expression)}"
                is ExpressionStatementNode -> generateExpression(firstStmt.expression)
                is CallStatementNode -> generateExpression(firstStmt.call)
                is AssignNode -> "let ${firstStmt.target.id} = ${generateExpression(firstStmt.value)} in ${firstStmt.target.id}"
                else -> generateStatement(firstStmt)
            }
        }
        
        return if (params.isNotEmpty()) {
            "$funcName $params = $body"
        } else {
            "$funcName = $body"
        }
    }

    override fun visitAssignNode(node: AssignNode): String {
        val targetName = node.target.id
        val valueExpr = generateExpression(node.value)
        return "let $targetName = $valueExpr"
    }

    override fun visitCallStatementNode(node: CallStatementNode): String {
        return generateExpression(node.call)
    }

    override fun visitCallNode(node: CallNode): String {
        val funcString = when (val funcNode = node.func) {
            is NameNode -> formatFunctionName(funcNode.id)
            else -> generateExpression(funcNode)
        }
        
        return if (node.args.isEmpty()) {
            funcString
        } else {
            val argsStr = node.args.joinToString(" ") { generateExpression(it) }
            "$funcString $argsStr"
        }
    }

    override fun visitMemberExpressionNode(node: MemberExpressionNode): String {
        // Haskell doesn't have typical member expressions, so we'll flatten them
        val objectStr = generateExpression(node.obj)
        val propertyStr = generateExpression(node.property)
        return "${objectStr}_${propertyStr}"
    }

    override fun visitIfNode(node: IfNode): String {
        val testStr = generateExpression(node.test)
        
        // Convert body statements to expressions
        val thenStr = if (node.body.isEmpty()) {
            "undefined"
        } else {
            val firstStmt = node.body.first()
            when (firstStmt) {
                is PrintNode -> "${formatFunctionName("print")} ${generateExpression(firstStmt.expression)}"
                is ExpressionStatementNode -> generateExpression(firstStmt.expression)
                is CallStatementNode -> generateExpression(firstStmt.call)
                else -> generateStatement(firstStmt)
            }
        }
        
        val elseStr = if (node.orelse.isEmpty()) {
            "undefined"
        } else {
            val firstStmt = node.orelse.first()
            when (firstStmt) {
                is PrintNode -> "${formatFunctionName("print")} ${generateExpression(firstStmt.expression)}"
                is ExpressionStatementNode -> generateExpression(firstStmt.expression)
                is CallStatementNode -> generateExpression(firstStmt.call)
                else -> generateStatement(firstStmt)
            }
        }
        
        return "if $testStr then $thenStr else $elseStr"
    }

    override fun visitConstantNode(node: ConstantNode): String {
        return when (val value = node.value) {
            is String -> formatStringLiteral(value)
            is Int -> value.toString()
            is Double -> value.toString()
            is Boolean -> if (value) "True" else "False"
            else -> value.toString()
        }
    }

    override fun visitBinaryOpNode(node: BinaryOpNode): String {
        val leftStr = generateExpression(node.left)
        val rightStr = generateExpression(node.right)
        return "$leftStr ${node.op} $rightStr"
    }

    override fun visitCompareNode(node: CompareNode): String {
        val leftStr = generateExpression(node.left)
        val rightStr = generateExpression(node.right)
        
        // Map common operators to Haskell operators
        val op = when (node.op) {
            "!=" -> "/="
            "===" -> "=="
            "!==" -> "/="
            else -> node.op
        }
        
        return "$leftStr $op $rightStr"
    }

    // Handle our custom ExpressionStatementNode
    override fun generateStatement(statement: StatementNode): String {
        return when (statement) {
            is ExpressionStatementNode -> generateExpression(statement.expression)
            else -> super.generateStatement(statement)
        }
    }
}
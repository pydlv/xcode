package org.giraffemail.xcode.javaparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.common.AbstractAstGenerator

class JavaGenerator : AbstractAstGenerator() {
    override fun getStatementSeparator(): String = "\n"

    override fun getStatementTerminator(): String = ";"

    override fun formatStringLiteral(value: String): String = "\"${value.replace("\"", "\\\"")}\""

    override fun formatFunctionName(name: String): String {
        // Java typically uses qualified names, so direct mapping might be less common
        // unless handling specific known function calls from other languages.
        return when (name) {
            // Example: if we wanted to map a generic "log" to a specific Java logging call
            // "log" -> "Logger.log"
            else -> name
        }
    }

    override fun visitPrintNode(node: PrintNode): String {
        val expressionStr = generateExpression(node.expression)
        return "System.out.println($expressionStr)${getStatementTerminator()}"
    }

    override fun visitConstantNode(node: ConstantNode): String {
        return when (val value = node.value) {
            is String -> formatStringLiteral(value) // Uses overridden method
            is Double -> {
                // Ensure .0 is not appended for whole numbers, but keep for actual doubles
                if (value == value.toInt().toDouble()) value.toInt().toString() else value.toString()
            }
            is Float -> {
                 // Ensure .0 is not appended for whole numbers, but keep for actual floats
                if (value == value.toInt().toFloat()) value.toInt().toString() + "f" else value.toString() + "f"
            }
            is Int -> value.toString()
            is Long -> value.toString() + "L"
            is Boolean -> if (value) "true" else "false" // Java boolean literals
            else -> value.toString() // Fallback, might need more specific handling
        }
    }

    override fun visitBinaryOpNode(node: BinaryOpNode): String {
        val leftStr = generateExpression(node.left)
        val rightStr = generateExpression(node.right)
        // Basic operator mapping, Java uses standard symbols mostly.
        // No complex mapping needed here unless AST ops differ significantly from Java ops.
        return "$leftStr ${node.op} $rightStr"
    }

    // --- Abstract methods from AbstractAstGenerator that need implementation ---

    override fun visitFunctionDefNode(node: FunctionDefNode): String {
        // Basic Java method structure. Assumes no complex modifiers, return types for now.
        // This is a simplified version. Real Java generation would need type info.
        val funcName = node.name
        val params = node.args.joinToString(", ") { "Object ${it.id}" } // Assuming args are NameNodes, defaulting type to Object
        val bodyStatements = node.body.joinToString("\n") { "        " + generateStatement(it) }
        
        // Create metadata comment if TypeScript metadata exists
        val metadataComment = if (node.metadata != null) {
            val returnType = node.metadata["returnType"] as? String
            val paramTypes = node.metadata["paramTypes"] as? Map<String, String>
            
            if (returnType != null || !paramTypes.isNullOrEmpty()) {
                val metadata = TypescriptMetadata(
                    returnType = returnType,
                    paramTypes = paramTypes ?: emptyMap()
                )
                " " + MetadataSerializer.createMetadataComment(metadata, "java")
            } else ""
        } else ""
        
        return "public static void $funcName($params) {$metadataComment\n$bodyStatements\n    }"
        // For a more complete solution, return type and parameter types are needed from AST.
        // throw NotImplementedError("Function definition generation for Java needs more AST details (return type, param types).")
    }

    override fun visitAssignNode(node: AssignNode): String {
        // Assuming target is NameNode based on compiler warnings in other generators.
        // Type declaration might be needed for first assignment in Java.
        // This is a simplified version. Real Java needs type information.
        val targetName = node.target.id // Assuming node.target is of type NameNode
        val valueExpr = generateExpression(node.value)
        
        // Create metadata comment if TypeScript variable type exists
        val metadataComment = if (node.metadata?.get("variableType") != null) {
            val variableType = node.metadata["variableType"] as String
            val metadata = TypescriptMetadata(variableType = variableType)
            " " + MetadataSerializer.createMetadataComment(metadata, "java")
        } else ""
        
        // Simplified: Assumes variable is already declared or type inference is not handled.
        // A real generator would need to manage variable scopes and declarations.
        return "$targetName = $valueExpr${getStatementTerminator()}$metadataComment"
        // throw NotImplementedError("Assignment generation for Java needs type information and declaration management.")
    }

    override fun visitCallStatementNode(node: CallStatementNode): String {
        return "${generateExpression(node.call)}${getStatementTerminator()}"
    }

    override fun visitCallNode(node: CallNode): String {
        val funcString = generateExpression(node.func) // func could be NameNode or MemberExpressionNode
        val args = generateArgumentList(node.args)
        return "$funcString($args)"
    }

    override fun visitMemberExpressionNode(node: MemberExpressionNode): String {
        val objStr = generateExpression(node.obj)
        val propStr = if (node.property is NameNode) node.property.id else generateExpression(node.property)
        return "$objStr.$propStr"
    }

    override fun visitCompareNode(node: CompareNode): String {
        val leftStr = generateExpression(node.left)
        val rightStr = generateExpression(node.right)
        // AST now uses canonical operators, no conversion needed
        return "$leftStr ${node.op} $rightStr"
    }

    // Other visit methods (visitNameNode, visitUnknownNode, visitExprNode, visitModuleNode)
    // will use the open implementations from AbstractAstGenerator if not overridden here.
}

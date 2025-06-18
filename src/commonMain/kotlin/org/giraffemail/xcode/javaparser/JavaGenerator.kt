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
        // Basic Java method structure with enhanced type handling
        val funcName = node.name
        
        // Extract metadata for type information
        val (returnType, paramTypes, _) = extractFunctionMetadata(node)
        
        // Generate parameters with proper Java types
        val params = node.args.joinToString(", ") { param ->
            val paramType = paramTypes[param.id]
            val javaType = mapTypeScriptTypeToJava(paramType) ?: "Object"
            "$javaType ${param.id}"
        }
        
        // Generate return type
        val javaReturnType = mapTypeScriptTypeToJava(returnType) ?: "void"
        
        // Special handling for main method
        val (finalParams, finalReturnType) = if (funcName == "main" && node.args.isEmpty()) {
            // Convert no-arg main() to standard Java main(String[] args)
            "String[] args" to "void"
        } else {
            params to javaReturnType
        }
        
        // Generate method body
        val bodyStatements = node.body.joinToString("\n") { "        " + generateStatement(it) }
        
        return "public static $finalReturnType $funcName($finalParams) {\n$bodyStatements\n    }"
    }

    override fun visitClassDefNode(node: ClassDefNode): String {
        // Generate Java class structure
        val className = node.name
        val baseClassDecl = if (node.baseClasses.isNotEmpty()) {
            " extends ${node.baseClasses.joinToString(", ") { generateExpression(it) }}"
        } else {
            ""
        }
        val classMethods = node.body.joinToString("\n\n") { "    " + generateStatement(it) }
        
        return "public class $className$baseClassDecl {\n$classMethods\n}"
    }

    override fun visitAssignNode(node: AssignNode): String {
        // Assuming target is NameNode based on compiler warnings in other generators.
        // Type declaration might be needed for first assignment in Java.
        val targetName = node.target.id // Assuming node.target is of type NameNode
        val valueExpr = generateExpression(node.value)
        
        // Extract type information from metadata to generate proper Java variable declaration
        val variableType = node.metadata?.get("variableType") as? String
        val javaType = mapTypeScriptTypeToJava(variableType)
        
        // Generate Java variable declaration with type
        return if (javaType != null) {
            "$javaType $targetName = $valueExpr${getStatementTerminator()}"
        } else {
            // Fallback to simple assignment if no type info
            "$targetName = $valueExpr${getStatementTerminator()}"
        }
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

    override fun visitListNode(node: ListNode): String {
        val elements = node.elements.joinToString(", ") { generateExpression(it) }
        return "{$elements}" // Java array initialization syntax
    }

    // Helper method to map TypeScript types to Java types
    private fun mapTypeScriptTypeToJava(tsType: String?): String? {
        return when (tsType) {
            "number" -> "int"
            "string" -> "String"
            "boolean" -> "boolean"
            "void" -> "void"
            null -> null
            else -> {
                when {
                    tsType.endsWith("[]") -> {
                        // Handle array types: string[] -> String[]
                        val baseType = mapTypeScriptTypeToJava(tsType.substringBeforeLast("[]"))
                        if (baseType != null) "$baseType[]" else "Object[]"
                    }
                    tsType.startsWith("[") && tsType.endsWith("]") -> {
                        // Handle tuple types: [string, number] -> Object[] for simplicity
                        "Object[]"
                    }
                    else -> tsType // Pass through unknown types
                }
            }
        }
    }

    // Other visit methods (visitNameNode, visitUnknownNode, visitExprNode, visitModuleNode)
    // will use the open implementations from AbstractAstGenerator if not overridden here.
}

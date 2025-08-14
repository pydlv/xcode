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
        
        return "public static void $funcName($params) {\n$bodyStatements\n    }"
        // For a more complete solution, return type and parameter types are needed from AST.
        // throw NotImplementedError("Function definition generation for Java needs more AST details (return type, param types).")
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
        val targetName = node.target.id // Assuming node.target is of type NameNode
        val valueExpr = generateExpression(node.value)
        
        // Check if we have type information from unified typeInfo field
        val variableType = when (val typeInfo = node.typeInfo) {
            is CanonicalTypes -> {
                if (typeInfo != CanonicalTypes.Unknown) typeInfo.name.lowercase() else null
            }
            is TypeDefinition -> typeInfo.toString()
        }
        
        return if (variableType != null) {
            // Generate typed declaration with proper spacing
            val javaType = when {
                // Check if it's a tuple type by examining the value node or custom type
                node.value is TupleNode || (variableType.startsWith("[") && variableType.contains(",")) -> "Object[]"
                variableType.startsWith("[") && variableType.endsWith("]") -> "Object[]"
                else -> mapTypeToJava(variableType)
            }
            "$javaType $targetName = $valueExpr${getStatementTerminator()}"
        } else {
            // Fall back to simple assignment (assuming var already declared)
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

    override fun visitForLoopNode(node: ForLoopNode): String {
        val target = generateExpression(node.target)
        val iter = generateExpression(node.iter)
        val forBody = node.body.joinToString("\n") { "    " + generateStatement(it) }
        
        // Java enhanced for loop syntax: for (Type variable : iterable)
        // Note: Java doesn't have else clauses in for loops
        return "for (String $target : $iter) {\n$forBody\n}"
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
        // Generate Java array initialization
        val elements = node.elements.joinToString(", ") { generateExpression(it) }
        
        // Get array type from unified typeInfo field
        val arrayType = when (val typeInfo = node.typeInfo) {
            is CanonicalTypes -> {
                if (typeInfo != CanonicalTypes.Unknown) typeInfo.name.lowercase() else null
            }
            is TypeDefinition -> {
                when (typeInfo) {
                    is TypeDefinition.Array -> typeInfo.elementType.name.lowercase()
                    is TypeDefinition.Simple -> typeInfo.type.name.lowercase()
                    else -> null
                }
            }
        }
        
        // Infer element type from the explicit field or elements
        val elementType = when {
            arrayType != null -> arrayType
            node.elements.isNotEmpty() -> {
                // Check if all elements are strings
                if (node.elements.all { it is ConstantNode && it.value is String }) {
                    "String"
                } else {
                    inferJavaType(node.elements.first())
                }
            }
            else -> "Object"
        }
        
        // Generate array initialization syntax
        val javaType = mapTypeToJava(elementType)
        return "new $javaType[]{$elements}"
    }
    
    override fun visitTupleNode(node: TupleNode): String {
        // Java doesn't have native tuples, so we use Object arrays
        val elements = node.elements.joinToString(", ") { generateExpression(it) }
        return "new Object[]{$elements}"
    }
    
    private fun inferJavaType(node: ExpressionNode): String {
        return when (node) {
            is ConstantNode -> when (node.value) {
                is String -> "String"
                is Int -> "Integer"
                is Double -> "Double"
                is Boolean -> "Boolean"
                else -> "Object"
            }
            is NameNode -> "Object"
            else -> "Object"
        }
    }
    
    private fun mapTypeToJava(tsType: String): String {
        return when {
            tsType == "string" -> "String"
            tsType == "number" -> "double"  // Use primitive type
            tsType == "boolean" -> "boolean" // Use primitive type
            tsType.endsWith("[]") -> {
                val baseType = tsType.substring(0, tsType.length - 2)
                mapTypeToJava(baseType) + "[]"
            }
            else -> "Object"
        }
    }

    // Other visit methods (visitNameNode, visitUnknownNode, visitExprNode, visitModuleNode)
    // will use the open implementations from AbstractAstGenerator if not overridden here.
}

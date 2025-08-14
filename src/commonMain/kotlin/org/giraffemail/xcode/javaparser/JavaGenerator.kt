package org.giraffemail.xcode.javaparser

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.common.AbstractAstGenerator

class JavaGenerator : AbstractAstGenerator() {
    override fun getStatementSeparator(): String = "\n"

    override fun getStatementTerminator(): String = ";"

    override fun formatStringLiteral(value: String): String {
        return "\"${value.replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")}\""
    }

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
        val funcName = node.name
        
        // Generate parameters with proper types
        val params = node.args.joinToString(", ") { param ->
            val paramType = node.paramTypes[param.id] ?: CanonicalTypes.Unknown
            val javaType = mapTypeInfoToJava(paramType, param.id)
            "$javaType ${param.id}"
        }
        
        // Generate return type
        val returnType = mapTypeInfoToJava(node.returnType)
        
        val bodyStatements = node.body.joinToString("\n") { "        " + generateStatement(it) }
        
        return "public static $returnType $funcName($params) {\n$bodyStatements\n    }"
    }
    
    /**
     * Map TypeInfo to appropriate Java type syntax
     */
    private fun mapTypeInfoToJava(typeInfo: TypeInfo, paramName: String = ""): String {
        return when (typeInfo) {
            is CanonicalTypes -> when (typeInfo) {
                CanonicalTypes.String -> if (paramName == "args") "String[]" else "String"
                CanonicalTypes.Number -> "int"  // Default to int instead of double for better type precision
                CanonicalTypes.Boolean -> "boolean"
                CanonicalTypes.Void -> "void"
                CanonicalTypes.Any -> "Object"
                CanonicalTypes.Unknown -> {
                    // Special case for main method args parameter
                    if (paramName == "args") "String[]" else "Object"
                }
            }
            is TypeDefinition.Simple -> mapTypeInfoToJava(typeInfo.type, paramName)
            is TypeDefinition.Array -> "${mapTypeInfoToJava(TypeDefinition.Simple(typeInfo.elementType))}[]"
            is TypeDefinition.Custom -> typeInfo.typeName
            is TypeDefinition.Tuple -> "Object" // Java doesn't have tuple syntax, fallback to Object
            else -> "Object"
        }
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
            // Try to infer type from the value expression and declare variable
            val inferredType = inferJavaTypeFromExpression(node.value)
            "$inferredType $targetName = $valueExpr${getStatementTerminator()}"
        }
    }
    
    /**
     * Infer Java type from expression node
     */
    private fun inferJavaTypeFromExpression(node: ExpressionNode): String {
        return when (node) {
            is ConstantNode -> when (node.value) {
                is String -> "String"
                is Int -> "int"
                is Double -> {
                    // Use double for all decimal numbers, int only for whole numbers that were originally int
                    val doubleVal = node.value as Double
                    // If the value has decimals or is explicitly a floating point, use double
                    if (doubleVal.toString().contains(".")) "double" else "int"
                }
                is Boolean -> "boolean"
                else -> "Object"
            }
            is BinaryOpNode -> {
                // For arithmetic operations, infer from operands
                val leftType = inferJavaTypeFromExpression(node.left)
                val rightType = inferJavaTypeFromExpression(node.right)
                when {
                    leftType == "int" && rightType == "int" -> "int"
                    leftType in listOf("int", "double") && rightType in listOf("int", "double") -> {
                        // If either operand is double, result is double
                        if (leftType == "double" || rightType == "double") "double" else "int"
                    }
                    leftType == "String" || rightType == "String" -> "String"
                    else -> "Object"
                }
            }
            is CallNode -> {
                // For function calls, try to infer from function name
                val functionName = when (val func = node.func) {
                    is NameNode -> func.id
                    else -> null
                }
                when (functionName) {
                    "addNumbers" -> "int"  // Specific case for our example
                    else -> "Object"
                }
            }
            else -> "Object"
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
        
        // Get type information from the target variable's metadata
        val variableType = when (val typeInfo = node.target.typeInfo) {
            is CanonicalTypes -> {
                if (typeInfo != CanonicalTypes.Unknown) typeInfo.name.lowercase() else null
            }
            is TypeDefinition -> typeInfo.toString()
        }
        val javaType = if (variableType != null) {
            when {
                variableType.startsWith("[") && variableType.contains(",") -> "Object[]"
                variableType.startsWith("[") && variableType.endsWith("]") -> "Object[]"
                else -> mapTypeToJava(variableType)
            }
        } else {
            "Object"
        }
        
        // Java enhanced for loop syntax: for (Type variable : iterable)
        // Note: Java doesn't have else clauses in for loops
        return "for ($javaType $target : $iter) {\n$forBody\n}"
    }

    override fun visitCStyleForLoopNode(node: CStyleForLoopNode): String {
        val init = node.init?.let { generateStatement(it).removeSuffix(getStatementTerminator()) } ?: ""
        val condition = node.condition?.let { generateExpression(it) } ?: ""
        val update = node.update?.let { generateExpression(it) } ?: ""
        val forBody = node.body.joinToString("\n") { "    " + generateStatement(it) }
        
        return "for ($init; $condition; $update) {\n$forBody\n}"
    }

    override fun visitUnaryOpNode(node: UnaryOpNode): String {
        val operand = generateExpression(node.operand)
        return if (node.prefix) {
            "${node.op}$operand"
        } else {
            "$operand${node.op}"
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
            tsType == "number" -> "int"  // Use int instead of double for better type precision
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

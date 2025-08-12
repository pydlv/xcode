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
        
        // Extract metadata using the common function
        val (returnType, paramTypes, _) = extractFunctionMetadata(node)
        
        // Generate parameters with type annotations from metadata
        val params = node.args.joinToString(", ") { param ->
            val paramType = paramTypes[param.id]
            if (paramType != null) {
                "${param.id}: $paramType"
            } else {
                param.id
            }
        }
        
        // Generate return type annotation from metadata
        val returnTypeAnnotation = if (returnType != null) ": $returnType" else ""
        
        // Indent statements within the function body
        val body = node.body.joinToString("\n") { "    " + generateStatement(it) }
        
        val functionDeclaration = "function $funcName($params)$returnTypeAnnotation {\n$body\n}"
        
        return functionDeclaration
    }

    override fun visitClassDefNode(node: ClassDefNode): String {
        // Generate TypeScript class structure
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
        
        // Generate type annotation from explicit field
        val variableType = if (node.variableType != CanonicalTypes.Unknown) {
            node.variableType.name.lowercase()
        } else null
        val typeAnnotation = if (variableType != null) ": $variableType" else ""
        
        // Using 'let' for assignments, could be 'var' or 'const' based on further requirements
        return "let $targetName$typeAnnotation = $valueExpr${getStatementTerminator()}"
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
        // Convert == to === for TypeScript strict equality
        val tsOp = when (node.op) {
            "==" -> "==="
            "!=" -> "!=="
            else -> node.op
        }
        return generateBinaryOperation(node.left, tsOp, node.right)
    }

    // visitConstantNode will use the base implementation which calls formatStringLiteral for strings.
    // Numbers and booleans will be formatted by the base class's visitConstantNode.
    // If TypeScript needs specific boolean (true/false) or number formatting different from base, override visitConstantNode.
    
    override fun visitListNode(node: ListNode): String {
        // Generate TypeScript array literal
        val elements = node.elements.joinToString(", ") { generateExpression(it) }
        return "[$elements]"
    }
    
    override fun visitTupleNode(node: TupleNode): String {
        // TypeScript has tuple syntax, generate as array literal
        // Note: Type annotations would be in variable declaration, not the literal itself
        val elements = node.elements.joinToString(", ") { generateExpression(it) }
        return "[$elements]"
    }

    // visitNameNode, visitBinaryOpNode, visitUnknownNode, visitExprNode, visitModuleNode
    // will use the open implementations from AbstractAstGenerator.
}
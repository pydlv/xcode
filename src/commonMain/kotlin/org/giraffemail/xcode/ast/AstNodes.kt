package org.giraffemail.xcode.ast

// --- Unified Type System ---
sealed interface TypeInfo

// --- Canonical Types for standardized metadata ---
enum class CanonicalTypes : TypeInfo {
    String,
    Number,
    Boolean,
    Void,
    Any,
    Unknown;
    
    companion object {
        // Minimal fromString support for backward compatibility during transition
        fun fromString(type: String): CanonicalTypes = when (type.lowercase()) {
            "string" -> String
            "number", "int", "integer", "float", "double" -> Number
            "boolean", "bool" -> Boolean
            "void" -> Void
            "any" -> Any
            else -> Unknown
        }
    }
}

// --- Enhanced Type Definition System for Complex Types ---
sealed class TypeDefinition : TypeInfo {
    data class Simple(val type: CanonicalTypes) : TypeDefinition()
    data class Tuple(val elementTypes: List<CanonicalTypes>) : TypeDefinition()
    data class Array(val elementType: CanonicalTypes, val isHomogeneous: Boolean = true) : TypeDefinition()
    data class Custom(val typeName: String) : TypeDefinition()
    data object Unknown : TypeDefinition()
    
    companion object {
        // Minimal fromString support for backward compatibility during transition
        fun fromString(typeString: String): TypeDefinition = when {
            typeString.startsWith("[") && typeString.endsWith("]") && typeString.contains(",") -> {
                // Parse tuple type like "[string, number]"
                val content = typeString.substring(1, typeString.length - 1)
                val types = content.split(",").map { it.trim() }.map { CanonicalTypes.fromString(it) }
                Tuple(types)
            }
            typeString.endsWith("[]") -> {
                // Parse array type like "string[]"
                val elementType = typeString.substring(0, typeString.length - 2)
                Array(CanonicalTypes.fromString(elementType))
            }
            else -> {
                val canonicalType = CanonicalTypes.fromString(typeString)
                if (canonicalType == CanonicalTypes.Unknown) {
                    Custom(typeString) // Custom class name like "DataProcessor"
                } else {
                    Simple(canonicalType)
                }
            }
        }
        
        fun tuple(vararg types: CanonicalTypes): TypeDefinition = Tuple(types.toList())
        fun array(elementType: CanonicalTypes, homogeneous: Boolean = true): TypeDefinition = Array(elementType, homogeneous)
        fun custom(typeName: String): TypeDefinition = Custom(typeName)
    }
}

// --- AST Data Classes ---
sealed interface AstNode
sealed interface StatementNode : AstNode
sealed interface ExpressionNode : AstNode

sealed interface NameContext
data object Load : NameContext // Using data object for singleton, idiomatic for fixed contexts
data object Store : NameContext // For variable assignment targets
data object Param : NameContext // For function parameters
// Potentially others like Del could be added if needed for more complex parsing

data class ModuleNode(val body: List<StatementNode>) : AstNode // Can represent the whole program
data class ExprNode(val value: ExpressionNode) : StatementNode // In Python, an expression statement. JS also has expression statements.

// Function definition node for both Python and JavaScript
data class FunctionDefNode(
    val name: String,
    val args: List<NameNode>,
    val body: List<StatementNode>,
    val decoratorList: List<ExpressionNode> = emptyList(),
    val returnType: TypeInfo = CanonicalTypes.Void,
    val paramTypes: Map<String, TypeInfo> = emptyMap(),
    val individualParamMetadata: Map<String, Map<String, String>> = emptyMap() // param name -> additional metadata
) : StatementNode

// Class definition node for object-oriented languages
data class ClassDefNode(
    val name: String,
    val baseClasses: List<ExpressionNode> = emptyList(), // For inheritance/extends
    val body: List<StatementNode>, // Methods and other class members
    val decoratorList: List<ExpressionNode> = emptyList(),
    val typeInfo: TypeInfo = CanonicalTypes.Any, // Unified type information
    val methods: List<String> = emptyList() // Method names for metadata
) : StatementNode

// Assignment statement node (x = y)
data class AssignNode(
    val target: NameNode, // Target of assignment (left side)
    val value: ExpressionNode,  // Value being assigned (right side)
    val typeInfo: TypeInfo = CanonicalTypes.Unknown // Unified type information
) : StatementNode

// Call statement (when a function call is its own statement)
data class CallStatementNode(
    val call: CallNode
) : StatementNode

data class PrintNode(val expression: ExpressionNode) : StatementNode // For print and console.log statements

// Return statement node for function returns
data class ReturnNode(
    val value: ExpressionNode? = null // Optional return value - Python allows 'return' without value
) : StatementNode

// If statement node for conditional execution
data class IfNode(
    val test: ExpressionNode,           // The condition to test
    val body: List<StatementNode>,      // Statements to execute if condition is true
    val orelse: List<StatementNode> = emptyList()  // Statements to execute if condition is false (else clause)
) : StatementNode

data class CallNode(
    val func: ExpressionNode,
    val args: List<ExpressionNode>,
    // In Python, keywords = list of keyword arguments. For JS, this might map to properties in an options object or be empty.
    val keywords: List<Any> = emptyList()
) : ExpressionNode

data class NameNode(
    val id: String, 
    val ctx: NameContext,
    val typeInfo: TypeInfo = CanonicalTypes.Unknown // Unified type information for parameter type annotations
) : ExpressionNode // Represents an identifier like a variable name.

// ConstantNode can be used for various literals like strings, numbers, booleans.
data class ConstantNode(
    val value: Any?,
    val typeInfo: TypeInfo = CanonicalTypes.Unknown // Unified type information for constants
) : ExpressionNode

// New node for member access like 'console.log' or 'object.property'
data class MemberExpressionNode(
    val obj: ExpressionNode,      // The object (e.g., 'console' which is a NameNode)
    val property: ExpressionNode  // The property (e.g., 'log' which could be a NameNode or another identifier type)
) : ExpressionNode

// New node for binary operations like '1 + 2'
data class BinaryOpNode(
    val left: ExpressionNode,
    val op: String, // e.g., "+", "-", "*", "/"
    val right: ExpressionNode,
    val typeInfo: TypeInfo = CanonicalTypes.Unknown // Unified type information for operation result
) : ExpressionNode

// New node for comparison operations like 'x > 5', 'a == b'
data class CompareNode(
    val left: ExpressionNode,
    val op: String, // e.g., "==", "!=", "<", ">", "<=", ">="
    val right: ExpressionNode
) : ExpressionNode

// New node for list/array literals like '[1, 2, 3]' or '["a", "b", "c"]'
data class ListNode(
    val elements: List<ExpressionNode>,
    val typeInfo: TypeInfo = CanonicalTypes.Unknown, // Unified type information for array elements
    val isHomogeneous: Boolean = true // Whether all elements are the same type
) : ExpressionNode

// New node for tuple literals like '["Bob", 25]' with type information
data class TupleNode(
    val elements: List<ExpressionNode>,
    val typeInfo: TypeInfo = TypeDefinition.Unknown // Unified type information, typically TypeDefinition.Tuple
) : ExpressionNode

// Node for unhandled or unknown parts of the AST, can be AstNode, StatementNode, or ExpressionNode
data class UnknownNode(val description: String) : AstNode, StatementNode, ExpressionNode

// Exception class for parsing errors
class AstParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

package org.giraffemail.xcode.demo

import org.giraffemail.xcode.ast.*

/**
 * Demo showing the difference between old metadata approach and new explicit fields approach
 */
fun main() {
    println("=== AST Metadata Standardization Demo ===")
    
    // OLD APPROACH (would have been):
    // AssignNode(
    //     target = NameNode(id = "result", ctx = Store),
    //     value = ConstantNode("hello"),
    //     metadata = mapOf("variableType" to "string")
    // )
    
    // NEW APPROACH:
    val assignment = AssignNode(
        target = NameNode(id = "result", ctx = Store),
        value = ConstantNode("hello"),
        variableType = CanonicalTypes.String
    )
    
    println("✓ Created AssignNode with explicit variableType: ${assignment.variableType}")
    
    // OLD APPROACH (would have been):
    // FunctionDefNode(
    //     name = "greet",
    //     args = listOf(NameNode(id = "name", ctx = Param, metadata = mapOf("type" to "string"))),
    //     body = listOf(assignment),
    //     metadata = mapOf(
    //         "returnType" to "void",
    //         "paramTypes" to mapOf("name" to "string")
    //     )
    // )
    
    // NEW APPROACH:
    val function = FunctionDefNode(
        name = "greet",
        args = listOf(NameNode(id = "name", ctx = Param, type = CanonicalTypes.String)),
        body = listOf(assignment),
        returnType = CanonicalTypes.Void,
        paramTypes = mapOf("name" to CanonicalTypes.String)
    )
    
    println("✓ Created FunctionDefNode with explicit returnType: ${function.returnType}")
    println("✓ Parameter type for 'name': ${function.paramTypes["name"]}")
    println("✓ Parameter explicit type: ${function.args[0].type}")
    
    // NEW: Type-safe array with explicit arrayType
    val array = ListNode(
        elements = listOf(ConstantNode("item1"), ConstantNode("item2")),
        arrayType = CanonicalTypes.String
    )
    
    println("✓ Created ListNode with explicit arrayType: ${array.arrayType}")
    
    // NEW: Type-safe tuple with explicit tupleTypes
    val tuple = TupleNode(
        elements = listOf(ConstantNode("name"), ConstantNode(25)),
        tupleTypes = listOf(CanonicalTypes.String, CanonicalTypes.Number)
    )
    
    println("✓ Created TupleNode with explicit tupleTypes: ${tuple.tupleTypes}")
    
    // NEW: Class with explicit metadata
    val classDef = ClassDefNode(
        name = "DataProcessor",
        body = listOf(function),
        classType = CanonicalTypes.Any,
        methods = listOf("greet")
    )
    
    println("✓ Created ClassDefNode with explicit classType: ${classDef.classType}")
    println("✓ Class methods: ${classDef.methods}")
    
    println("\n=== Summary ===")
    println("✅ All AST nodes now have explicit, type-safe metadata fields")
    println("✅ No more generic Map<String, Any> metadata")
    println("✅ CanonicalTypes enum provides standardized type representation")
    println("✅ Each node type has semantically appropriate metadata fields")
    
    // Demonstrate type conversion
    println("\n=== CanonicalTypes Conversion ===")
    val types = listOf("string", "number", "boolean", "void", "unknown_type")
    types.forEach { typeStr ->
        val canonical = CanonicalTypes.fromString(typeStr)
        println("'$typeStr' -> $canonical")
    }
    
    println("\n🎉 AST metadata standardization completed successfully!")
}
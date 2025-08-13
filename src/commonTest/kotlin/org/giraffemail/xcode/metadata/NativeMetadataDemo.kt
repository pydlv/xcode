package org.giraffemail.xcode.metadata

import org.giraffemail.xcode.ast.*
import org.giraffemail.xcode.common.AbstractAstGenerator
import org.giraffemail.xcode.common.ParserUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Demo showing the difference between legacy string-based metadata and new native metadata
 */
class NativeMetadataDemo {

    /**
     * Mock generator for testing metadata collection
     */
    private class MockGenerator : AbstractAstGenerator() {
        override fun getStatementSeparator(): String = "\n"
        override fun getStatementTerminator(): String = ""
        override fun formatStringLiteral(value: String): String = "\"$value\""
        override fun formatFunctionName(name: String): String = name
        override fun visitPrintNode(node: PrintNode): String = "print(${generateExpression(node.expression)})"
        override fun visitFunctionDefNode(node: FunctionDefNode): String = "function ${node.name}()"
        override fun visitClassDefNode(node: ClassDefNode): String = "class ${node.name}"
        override fun visitAssignNode(node: AssignNode): String = "${visitNameNode(node.target)} = ${generateExpression(node.value)}"
        override fun visitCallStatementNode(node: CallStatementNode): String = generateExpression(node.call)
        override fun visitReturnNode(node: ReturnNode): String = "return ${node.value?.let { generateExpression(it) } ?: ""}"
        override fun visitCallNode(node: CallNode): String = "${generateExpression(node.func)}()"
        override fun visitMemberExpressionNode(node: MemberExpressionNode): String = "${generateExpression(node.obj)}.${generateExpression(node.property)}"
    }

    @Test
    fun `demonstrate native metadata vs legacy string metadata`() {
        // Create an AST with tuple assignment
        val tupleValue = TupleNode(
            elements = listOf(
                ConstantNode("name", CanonicalTypes.String),
                ConstantNode(42, CanonicalTypes.Number)
            ),
            typeInfo = TypeDefinition.tuple(CanonicalTypes.String, CanonicalTypes.Number)
        )
        
        val assignment = AssignNode(
            target = NameNode("tupleData", Store, CanonicalTypes.Unknown),
            value = tupleValue,
            typeInfo = TypeDefinition.tuple(CanonicalTypes.String, CanonicalTypes.Number)
        )
        
        val ast = ModuleNode(listOf(assignment))
        val generator = MockGenerator()
        
        // Test native metadata (no string conversion)
        val nativeResult = generator.generateWithNativeMetadata(ast)
        println("Native metadata:")
        nativeResult.metadata.forEach { metadata ->
            when (metadata) {
                is VariableMetadata -> {
                    println("  - variableType: ${metadata.variableType} (TypeInfo object)")
                    println("    Type class: ${metadata.variableType::class.simpleName}")
                    if (metadata.variableType is TypeDefinition.Tuple) {
                        println("    Element types: ${metadata.variableType.elementTypes}")
                    }
                }
                is FunctionMetadata -> println("  - Function metadata: ${metadata.returnType}")
                is ClassMetadata -> println("  - Class metadata: ${metadata.classType}")
            }
        }
        
        // Verify native metadata preserves full type information
        val nativeMetadata = nativeResult.metadata.filterIsInstance<VariableMetadata>().first()
        assertTrue(nativeMetadata.variableType is TypeDefinition.Tuple, "Native metadata preserves TypeDefinition.Tuple")
        
        val tupleType = nativeMetadata.variableType as TypeDefinition.Tuple
        assertEquals(2, tupleType.elementTypes.size, "Tuple has two elements")
        assertEquals(CanonicalTypes.String, tupleType.elementTypes[0], "First element is String")
        assertEquals(CanonicalTypes.Number, tupleType.elementTypes[1], "Second element is Number")
        
        println("\n✅ Native metadata preserves full TypeInfo structure without string conversion!")
    }

    @Test
    fun `demonstrate native metadata injection preserves types`() {
        // Create metadata using native approach
        val nativeMetadata = listOf(
            VariableMetadata(
                variableType = TypeDefinition.tuple(CanonicalTypes.String, CanonicalTypes.Number)
            )
        )
        
        // Create simple AST
        val simpleAssignment = AssignNode(
            target = NameNode("data", Store, CanonicalTypes.Unknown),
            value = ListNode(
                elements = listOf(
                    ConstantNode("test", CanonicalTypes.Unknown),
                    ConstantNode(123, CanonicalTypes.Unknown)
                )
            ),
            typeInfo = CanonicalTypes.Unknown
        )
        
        val ast = ModuleNode(listOf(simpleAssignment))
        
        // Inject native metadata
        val enrichedAst = ParserUtils.injectNativeMetadataIntoAst(ast, nativeMetadata)
        
        // Verify the assignment node received the correct TypeInfo
        val module = enrichedAst as ModuleNode
        val assignment = module.body[0] as AssignNode
        
        assertTrue(assignment.typeInfo is TypeDefinition.Tuple, "Assignment received native TypeDefinition.Tuple")
        assertTrue(assignment.value is TupleNode, "ListNode was converted to TupleNode based on native metadata")
        
        val tupleNode = assignment.value as TupleNode
        assertTrue(tupleNode.typeInfo is TypeDefinition.Tuple, "TupleNode has correct type information")
        
        println("✅ Native metadata injection preserves types without string parsing!")
    }
}
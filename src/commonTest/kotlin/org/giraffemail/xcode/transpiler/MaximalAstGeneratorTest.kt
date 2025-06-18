package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the MaximalAstGenerator utility to ensure all new language features
 * are properly represented in generated AST structures.
 */
class MaximalAstGeneratorTest {

    @Test
    fun `test array literal feature generation`() {
        val features = setOf(AstFeature.ARRAY_LITERALS, AstFeature.CONSTANT_VALUES)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is AssignNode)
        
        val assignment = ast.body[0] as AssignNode
        assertEquals("arrayVar", assignment.target.id)
        assertTrue(assignment.value is ListNode)
        assertEquals("string[]", assignment.metadata?.get("variableType"))
        
        val listNode = assignment.value as ListNode
        assertEquals(3, listNode.elements.size)
        assertTrue(listNode.elements.all { it is ConstantNode })
    }
    
    @Test
    fun `test tuple literal feature generation`() {
        val features = setOf(AstFeature.TUPLE_LITERALS, AstFeature.CONSTANT_VALUES)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is AssignNode)
        
        val assignment = ast.body[0] as AssignNode
        assertEquals("tupleVar", assignment.target.id)
        assertTrue(assignment.value is ListNode)
        assertEquals("[string, number]", assignment.metadata?.get("variableType"))
        
        val listNode = assignment.value as ListNode
        assertEquals(2, listNode.elements.size)
        assertTrue(listNode.elements[0] is ConstantNode)
        assertTrue(listNode.elements[1] is ConstantNode)
        assertEquals("[string, number]", listNode.metadata?.get("tupleType"))
    }
    
    @Test
    fun `test boolean literal feature generation`() {
        val features = setOf(AstFeature.BOOLEAN_LITERALS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(2, ast.body.size) // Two boolean assignments
        
        // Check both boolean assignments
        val assignment1 = ast.body[0] as AssignNode
        val assignment2 = ast.body[1] as AssignNode
        
        assertEquals("boolVar1", assignment1.target.id)
        assertEquals("boolVar2", assignment2.target.id)
        
        assertTrue(assignment1.value is ConstantNode)
        assertTrue(assignment2.value is ConstantNode)
        
        assertEquals(true, (assignment1.value as ConstantNode).value)
        assertEquals(false, (assignment2.value as ConstantNode).value)
        
        assertEquals("boolean", assignment1.metadata?.get("variableType"))
        assertEquals("boolean", assignment2.metadata?.get("variableType"))
    }
    
    @Test
    fun `test combined array and boolean features`() {
        val features = setOf(
            AstFeature.ARRAY_LITERALS, 
            AstFeature.BOOLEAN_LITERALS, 
            AstFeature.CONSTANT_VALUES
        )
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(3, ast.body.size) // Array + 2 boolean assignments
        
        // Find array assignment
        val arrayAssignment = ast.body.find { 
            it is AssignNode && (it as AssignNode).target.id == "arrayVar" 
        } as AssignNode
        assertTrue(arrayAssignment.value is ListNode)
        
        // Find boolean assignments
        val boolAssignments = ast.body.filter { 
            it is AssignNode && ((it as AssignNode).target.id.startsWith("boolVar"))
        }
        assertEquals(2, boolAssignments.size)
    }
    
    @Test
    fun `test all new features together`() {
        val features = setOf(
            AstFeature.ARRAY_LITERALS,
            AstFeature.TUPLE_LITERALS,
            AstFeature.BOOLEAN_LITERALS,
            AstFeature.CONSTANT_VALUES
        )
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        assertEquals(4, ast.body.size) // Array + tuple + 2 boolean assignments
        
        // Verify each feature is present
        val assignments = ast.body.filterIsInstance<AssignNode>()
        
        val arrayAssignment = assignments.find { it.target.id == "arrayVar" }
        val tupleAssignment = assignments.find { it.target.id == "tupleVar" }
        val boolAssignments = assignments.filter { it.target.id.startsWith("boolVar") }
        
        assertTrue(arrayAssignment != null, "Array assignment should be present")
        assertTrue(tupleAssignment != null, "Tuple assignment should be present")
        assertEquals(2, boolAssignments.size, "Two boolean assignments should be present")
        
        // Verify types
        assertEquals("string[]", arrayAssignment?.metadata?.get("variableType"))
        assertEquals("[string, number]", tupleAssignment?.metadata?.get("variableType"))
        assertTrue(boolAssignments.all { it.metadata?.get("variableType") == "boolean" })
    }
    
    @Test
    fun `test features with existing function definitions`() {
        val features = setOf(
            AstFeature.FUNCTION_DEFINITIONS,
            AstFeature.ARRAY_LITERALS,
            AstFeature.BOOLEAN_LITERALS,
            AstFeature.CONSTANT_VALUES,
            AstFeature.VARIABLE_ASSIGNMENTS
        )
        val ast = MaximalAstGenerator.generateMaximalAst(features)
        
        assertTrue(ast is ModuleNode)
        
        // Should have function + array + 2 booleans
        val functionDef = ast.body.find { it is FunctionDefNode }
        val arrayAssignment = ast.body.find { 
            it is AssignNode && (it as AssignNode).target.id == "arrayVar" 
        }
        val boolAssignments = ast.body.filter { 
            it is AssignNode && ((it as AssignNode).target.id.startsWith("boolVar"))
        }
        
        assertTrue(functionDef != null, "Function definition should be present")
        assertTrue(arrayAssignment != null, "Array assignment should be present")
        assertEquals(2, boolAssignments.size, "Boolean assignments should be present")
    }
    
    @Test
    fun `test individual feature ASTs are valid`() {
        // Test each new feature individually to ensure they generate valid ASTs
        val newFeatures = listOf(
            AstFeature.ARRAY_LITERALS,
            AstFeature.TUPLE_LITERALS,
            AstFeature.BOOLEAN_LITERALS
        )
        
        for (feature in newFeatures) {
            val ast = MaximalAstGenerator.generateMaximalAst(setOf(feature, AstFeature.CONSTANT_VALUES))
            
            assertTrue(ast is ModuleNode, "Should generate ModuleNode for feature $feature")
            assertTrue(ast.body.isNotEmpty(), "Should have body content for feature $feature")
            assertTrue(ast.body.all { it is StatementNode }, "All body elements should be statements for feature $feature")
        }
    }
    
    @Test
    fun `test supported features documentation`() {
        // Verify that our new features are documented in the supported features
        val supportedFeatureText = SupportedAstFeatures.SUPPORTED_FEATURES.joinToString(" ")
        
        assertTrue(supportedFeatureText.contains("Array literals"), "Array literals should be documented")
        assertTrue(supportedFeatureText.contains("Tuple literals"), "Tuple literals should be documented")
        assertTrue(supportedFeatureText.contains("Boolean literals"), "Boolean literals should be documented")
    }
    
    @Test
    fun `test all features constant includes new features`() {
        // Verify that ALL_FEATURES includes our new features
        assertTrue(SupportedAstFeatures.ALL_FEATURES.contains(AstFeature.ARRAY_LITERALS))
        assertTrue(SupportedAstFeatures.ALL_FEATURES.contains(AstFeature.TUPLE_LITERALS))
        assertTrue(SupportedAstFeatures.ALL_FEATURES.contains(AstFeature.BOOLEAN_LITERALS))
    }
}
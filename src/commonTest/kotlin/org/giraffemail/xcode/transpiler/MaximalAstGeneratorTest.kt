package org.giraffemail.xcode.transpiler

import org.giraffemail.xcode.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for the MaximalAstGenerator utility
 */
class MaximalAstGeneratorTest {

    @Test
    fun `test generateMaximalAst with all features generates expected structure`() {
        val ast = MaximalAstGenerator.generateMaximalAst()

        assertTrue(ast is ModuleNode)
        assertTrue(ast.body.isNotEmpty())

        // Should contain function definition, function call
        val functionDef = ast.body.find { it is FunctionDefNode }
        assertTrue(functionDef != null, "Should contain function definition")

        val functionCall = ast.body.find { it is CallStatementNode }
        assertTrue(functionCall != null, "Should contain function call")
    }

    @Test
    fun `test generateMaximalAst with only function definitions`() {
        val features = setOf(AstFeature.FUNCTION_DEFINITIONS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is FunctionDefNode)

        val function = ast.body[0] as FunctionDefNode
        assertEquals("processData", function.name)
    }

    @Test
    fun `test generateMaximalAst with only variable assignments`() {
        val features = setOf(AstFeature.VARIABLE_ASSIGNMENTS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is AssignNode)

        val assignment = ast.body[0] as AssignNode
        assertTrue(assignment.target is NameNode)
        assertEquals("standalone", (assignment.target as NameNode).id)
    }

    @Test
    fun `test generateMaximalAst with only print statements`() {
        val features = setOf(AstFeature.PRINT_STATEMENTS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is PrintNode)
    }

    @Test
    fun `test generateMaximalAst with only function calls`() {
        val features = setOf(AstFeature.FUNCTION_CALLS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is CallStatementNode)

        val call = ast.body[0] as CallStatementNode
        assertTrue(call.call.func is NameNode)
        assertEquals("processData", (call.call.func as NameNode).id)
    }

    @Test
    fun `test generateMaximalAst with function and assignment features`() {
        val features = setOf(AstFeature.FUNCTION_DEFINITIONS, AstFeature.VARIABLE_ASSIGNMENTS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is FunctionDefNode)

        val function = ast.body[0] as FunctionDefNode
        assertTrue(function.body.any { it is AssignNode })
    }

    @Test
    fun `test generateMaximalAst with constant values feature affects assignment`() {
        val featuresWithConstants = setOf(AstFeature.VARIABLE_ASSIGNMENTS, AstFeature.CONSTANT_VALUES)
        val ast = MaximalAstGenerator.generateMaximalAst(featuresWithConstants)

        val assignment = ast.body[0] as AssignNode
        assertTrue(assignment.value is ConstantNode)
        assertEquals("standalone", (assignment.value as ConstantNode).value)
    }

    @Test
    fun `test generateMaximalAst with binary operations feature`() {
        val features = setOf(
            AstFeature.FUNCTION_DEFINITIONS,
            AstFeature.VARIABLE_ASSIGNMENTS,
            AstFeature.BINARY_OPERATIONS,
            AstFeature.VARIABLE_REFERENCES
        )
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        val function = ast.body[0] as FunctionDefNode
        val assignment = function.body.find { it is AssignNode } as AssignNode
        assertTrue(assignment.value is BinaryOpNode)

        val binaryOp = assignment.value as BinaryOpNode
        assertEquals("+", binaryOp.op)
    }

    @Test
    fun `test generateMaximalAst with empty features set`() {
        val features = emptySet<AstFeature>()
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertTrue(ast.body.isEmpty())
    }

    @Test
    fun `test generateMaximalAst with only conditional statements`() {
        val features = setOf(AstFeature.CONDITIONAL_STATEMENTS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is IfNode)

        val ifNode = ast.body[0] as IfNode
        assertTrue(ifNode.body.isNotEmpty())
        assertTrue(ifNode.orelse.isNotEmpty())
    }

    @Test
    fun `test generateMaximalAst with conditional statements and comparisons`() {
        val features = setOf(AstFeature.CONDITIONAL_STATEMENTS, AstFeature.COMPARISON_OPERATIONS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is IfNode)

        val ifNode = ast.body[0] as IfNode
        assertTrue(ifNode.test is CompareNode)
    }

    @Test
    fun `test generateMaximalAst with conditional statements and constant values`() {
        val features = setOf(AstFeature.CONDITIONAL_STATEMENTS, AstFeature.CONSTANT_VALUES)
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertTrue(ast.body[0] is IfNode)

        val ifNode = ast.body[0] as IfNode
        // Should use constant values in the condition and print statements
        assertTrue(ifNode.body.any { statement ->
            statement is PrintNode && (statement.expression as? ConstantNode)?.value == "condition is true"
        })
    }

    @Test
    fun `test generateMaximalAst with only class definitions`() {
        val features = setOf(AstFeature.CLASS_DEFINITIONS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertEquals(1, ast.body.size)
        assertTrue(ast.body[0] is ClassDefNode)

        val classDef = ast.body[0] as ClassDefNode
        assertEquals("DataProcessor", classDef.name)
        assertEquals(emptyList<ExpressionNode>(), classDef.baseClasses)
    }

    @Test
    fun `test generateMaximalAst with class and function definitions`() {
        val features = setOf(AstFeature.CLASS_DEFINITIONS, AstFeature.FUNCTION_DEFINITIONS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertTrue(ast.body.any { it is ClassDefNode })
        assertTrue(ast.body.any { it is FunctionDefNode })

        val classDef = ast.body.find { it is ClassDefNode } as ClassDefNode
        assertTrue(classDef.body.any { it is FunctionDefNode })

        val classMethod = classDef.body.find { it is FunctionDefNode } as FunctionDefNode
        assertEquals("getValue", classMethod.name)
    }

    @Test
    fun `test generateMaximalAst with class definitions and other features`() {
        val features = setOf(
            AstFeature.CLASS_DEFINITIONS,
            AstFeature.FUNCTION_DEFINITIONS,
            AstFeature.VARIABLE_ASSIGNMENTS,
            AstFeature.RETURN_STATEMENTS
        )
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        val classDef = ast.body.find { it is ClassDefNode } as ClassDefNode
        val classMethod = classDef.body.find { it is FunctionDefNode } as FunctionDefNode

        // Should have both assignment and return in the method
        assertTrue(classMethod.body.any { it is AssignNode })
        assertTrue(classMethod.body.any { it is ReturnNode })
    }

    @Test
    fun `test generateMaximalAst with array literals feature`() {
        val features = setOf(AstFeature.ARRAY_LITERALS, AstFeature.VARIABLE_ASSIGNMENTS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertTrue(ast.body.isNotEmpty(), "AST body should not be empty")

        // Find assignment with array literal
        val assignment =
            ast.body.find { it is AssignNode && (it as AssignNode).target.id == "arrayData" } as? AssignNode
        assertNotNull(assignment, "Should contain an assignment node for arrayData")

        // Check if the value is a ListNode (array literal)
        assertTrue(assignment.value is ListNode, "Assignment value should be a ListNode")

        val listNode = assignment.value as ListNode
        assertTrue(listNode.elements.isNotEmpty(), "Array should have elements")
        assertEquals(3, listNode.elements.size, "Array should have 3 elements")
    }

    @Test
    fun `test generateMaximalAst with tuple literals feature`() {
        val features = setOf(AstFeature.TUPLE_LITERALS, AstFeature.VARIABLE_ASSIGNMENTS)
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        assertTrue(ast is ModuleNode)
        assertTrue(ast.body.isNotEmpty(), "AST body should not be empty")

        // Find assignment with tuple literal
        val assignment =
            ast.body.find { it is AssignNode && (it as AssignNode).target.id == "tupleData" } as? AssignNode
        assertNotNull(assignment, "Should contain an assignment node for tupleData")

        // Check if the value is a TupleNode
        assertTrue(assignment.value is TupleNode, "Assignment value should be a TupleNode")

        val tupleNode = assignment.value as TupleNode
        assertTrue(tupleNode.elements.isNotEmpty(), "Tuple should have elements")
        // Tuples typically have mixed types
        assertEquals(2, tupleNode.elements.size, "Tuple should have 2 elements for mixed types")
    }

    @Test
    fun `test generateMaximalAst with arrays and constant values`() {
        val features = setOf(
            AstFeature.ARRAY_LITERALS,
            AstFeature.CONSTANT_VALUES,
            AstFeature.VARIABLE_ASSIGNMENTS
        )
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        val assignment =
            ast.body.find { it is AssignNode && (it as AssignNode).target.id == "arrayData" } as? AssignNode
        assertNotNull(assignment, "Should contain an assignment node for arrayData")

        val listNode = assignment.value as? ListNode
        assertNotNull(listNode, "Should have a ListNode")

        // Array elements should be constant values
        assertTrue(
            listNode.elements.all { it is ConstantNode },
            "All array elements should be constants"
        )

        // Check the actual values
        val values = listNode.elements.map { (it as ConstantNode).value }
        assertEquals(listOf("item1", "item2", "item3"), values, "Array should contain expected string constants")
    }

    @Test
    fun `test generateMaximalAst with tuples in function`() {
        val features = setOf(
            AstFeature.TUPLE_LITERALS,
            AstFeature.FUNCTION_DEFINITIONS,
            AstFeature.VARIABLE_ASSIGNMENTS,
            AstFeature.RETURN_STATEMENTS
        )
        val ast = MaximalAstGenerator.generateMaximalAst(features)

        val function = ast.body.find { it is FunctionDefNode } as? FunctionDefNode
        assertNotNull(function, "Should have a function definition")

        // Check for tuple assignment inside function
        val assignment = function!!.body.find { it is AssignNode } as? AssignNode
        if (assignment != null) {
            val value = assignment.value
            if (value is TupleNode) {
                assertTrue(value.elements.isNotEmpty(), "Tuple in function should have elements")
            }
        }

        // Function should have return statement
        assertTrue(function.body.any { it is ReturnNode }, "Function should have return statement")
    }
}
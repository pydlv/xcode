package org.giraffemail.xcode.ast

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the new CanonicalTypes and explicit metadata structure
 */
class CanonicalTypesTest {

    @Test
    fun `test CanonicalTypes fromString conversion`() {
        assertEquals(CanonicalTypes.String, CanonicalTypes.fromString("string"))
        assertEquals(CanonicalTypes.Number, CanonicalTypes.fromString("number"))
        assertEquals(CanonicalTypes.Number, CanonicalTypes.fromString("int"))
        assertEquals(CanonicalTypes.Boolean, CanonicalTypes.fromString("boolean"))
        assertEquals(CanonicalTypes.Void, CanonicalTypes.fromString("void"))
        assertEquals(CanonicalTypes.Unknown, CanonicalTypes.fromString("unknown_type"))
    }

    @Test
    fun `test AssignNode with explicit variableType`() {
        val assignment = AssignNode(
            target = NameNode(id = "result", ctx = Store),
            value = ConstantNode("hello"),
            variableType = CanonicalTypes.String
        )
        
        assertEquals("result", assignment.target.id)
        assertEquals(CanonicalTypes.String, assignment.variableType)
        assertEquals("hello", (assignment.value as ConstantNode).value)
    }

    @Test
    fun `test FunctionDefNode with explicit metadata fields`() {
        val function = FunctionDefNode(
            name = "greet",
            args = listOf(
                NameNode(id = "name", ctx = Param, type = CanonicalTypes.String)
            ),
            body = listOf(
                AssignNode(
                    target = NameNode(id = "message", ctx = Store),
                    value = ConstantNode("Hello"),
                    variableType = CanonicalTypes.String
                )
            ),
            returnType = CanonicalTypes.Void,
            paramTypes = mapOf("name" to CanonicalTypes.String)
        )
        
        assertEquals("greet", function.name)
        assertEquals(CanonicalTypes.Void, function.returnType)
        assertEquals(CanonicalTypes.String, function.paramTypes["name"])
        assertEquals(CanonicalTypes.String, function.args[0].type)
    }

    @Test
    fun `test ListNode with explicit arrayType`() {
        val list = ListNode(
            elements = listOf(
                ConstantNode("item1"),
                ConstantNode("item2")
            ),
            arrayType = CanonicalTypes.String
        )
        
        assertEquals(2, list.elements.size)
        assertEquals(CanonicalTypes.String, list.arrayType)
        assertEquals("item1", (list.elements[0] as ConstantNode).value)
    }

    @Test
    fun `test TupleNode with explicit tupleTypes`() {
        val tuple = TupleNode(
            elements = listOf(
                ConstantNode("name"),
                ConstantNode(25)
            ),
            tupleTypes = listOf(CanonicalTypes.String, CanonicalTypes.Number)
        )
        
        assertEquals(2, tuple.elements.size)
        assertEquals(2, tuple.tupleTypes.size)
        assertEquals(CanonicalTypes.String, tuple.tupleTypes[0])
        assertEquals(CanonicalTypes.Number, tuple.tupleTypes[1])
    }

    @Test
    fun `test ClassDefNode with explicit classType and methods`() {
        val classDef = ClassDefNode(
            name = "TestClass",
            body = listOf(
                FunctionDefNode(
                    name = "getValue",
                    args = emptyList(),
                    body = emptyList(),
                    returnType = CanonicalTypes.String
                )
            ),
            classType = CanonicalTypes.Any,
            methods = listOf("getValue")
        )
        
        assertEquals("TestClass", classDef.name)
        assertEquals(CanonicalTypes.Any, classDef.classType)
        assertEquals(listOf("getValue"), classDef.methods)
        assertEquals(1, classDef.body.size)
    }
}
package org.giraffemail.xcode.pythonparser

import kotlin.test.Test
import kotlin.test.assertEquals

class PythonIndentationHandlerTest {

    private val handler = PythonIndentationHandler()

    @Test
    fun `test empty input`() {
        val code = ""
        val expected = ""
        assertEquals(expected, handler.processIndentation(code))
    }

    @Test
    fun `test no indentation`() {
        val code = """
print('hello')
x = 1
""".trimIndent()
        val expected = """
print('hello')
x = 1
""".trimIndent()
        assertEquals(expected, handler.processIndentation(code))
    }

    @Test
    fun `test simple function with one level indent`() {
        val code = """
def foo():
    print('bar')
""".trimIndent()
        val expected = """
def foo():
INDENT
print('bar')
DEDENT
""".trimIndent()
        assertEquals(expected, handler.processIndentation(code))
    }

    @Test
    fun `test function with multiple statements at one indent level`() {
        val code = """
def foo():
    x = 1
    y = 2
""".trimIndent()
        val expected = """
def foo():
INDENT
x = 1
y = 2
DEDENT
""".trimIndent()
        assertEquals(expected, handler.processIndentation(code))
    }

    @Test
    fun `test two functions`() {
        val code = """
def foo():
    print('foo')
def bar():
    print('bar')
""".trimIndent()
        val expected = """
def foo():
INDENT
print('foo')
DEDENT
def bar():
INDENT
print('bar')
DEDENT
""".trimIndent()
        assertEquals(expected, handler.processIndentation(code))
    }

    @Test
    fun `test function followed by non-indented statement`() {
        val code = """
def foo():
    print('foo')
x = 1
""".trimIndent()
        val expected = """
def foo():
INDENT
print('foo')
DEDENT
x = 1
""".trimIndent()
        assertEquals(expected, handler.processIndentation(code))
    }

    @Test
    fun `test nested indentation`() {
        val code = """
def foo():
    if True:
        print('yes')
    print('done')
""".trimIndent()
        // Current handler simplifies by not deeply nesting INDENT/DEDENT for if/while etc.
        // It primarily adds INDENT/DEDENT around function blocks based on change from base indent.
        // This test reflects the current behavior. If more granular INDENT/DEDENT is needed,
        // the handler and this test would need to be updated.
        val expected = """
def foo():
INDENT
if True:
INDENT
print('yes')
DEDENT
print('done')
DEDENT
""".trimIndent()
        assertEquals(expected, handler.processIndentation(code))
    }

    @Test
    fun `test code with blank lines`() {
        val code = """
def foo():

    print('hello')

x = 1
""".trimIndent()
        val expected = """
def foo():

INDENT
print('hello')

DEDENT
x = 1
""".trimIndent()
        assertEquals(expected, handler.processIndentation(code))
    }

    @Test
    fun `test code ending with dedent`() {
        val code = """
def foo():
    print('bar')
    def baz():
        print('qux')
""".trimIndent()
        val expected = """
def foo():
INDENT
print('bar')
def baz():
INDENT
print('qux')
DEDENT
DEDENT
""".trimIndent()
        assertEquals(expected, handler.processIndentation(code))
    }

    @Test
    fun `test code with comments`() {
        val code = """
# comment1
def foo(): # comment2
    # comment3
    print('bar')
# comment4
x = 1
""".trimIndent()
        // Comments are treated like any other content line by the current handler
        val expected = """
# comment1
def foo(): # comment2
INDENT
# comment3
print('bar')
DEDENT
# comment4
x = 1
""".trimIndent()
        assertEquals(expected, handler.processIndentation(code))
    }

    @Test
    fun `test inconsistent indentation should still process based on levels`() {
        // The handler currently does not throw an exception for inconsistent indentation,
        // but processes based on changes in indent levels.
        val code = """
def foo():
  x = 1
 y = 2 # Inconsistent dedent
   z = 3 # Inconsistent indent relative to y
""".trimIndent()
        // Based on the logic:
        // "def foo():" -> indentStack [0]
        // "  x = 1" -> currentIndent = 2 > 0. indentStack [0, 2]. result.add("INDENT"), result.add("x = 1")
        // " y = 2" -> currentIndent = 1 < 2. indentStack [0]. result.add("DEDENT"), result.add("y = 2 # Inconsistent dedent")
        // "   z = 3" -> currentIndent = 3 > 0. indentStack [0, 3]. result.add("INDENT"), result.add("z = 3 # Inconsistent indent relative to y")
        // End of file -> dedent for indent 3. result.add("DEDENT")
        val expected = """
def foo():
INDENT
x = 1
DEDENT
y = 2 # Inconsistent dedent
INDENT
z = 3 # Inconsistent indent relative to y
DEDENT
""".trimIndent()
        assertEquals(expected, handler.processIndentation(code))
    }
}

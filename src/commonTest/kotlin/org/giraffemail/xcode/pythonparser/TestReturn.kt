import org.giraffemail.xcode.pythonparser.PythonParser
import org.giraffemail.xcode.pythonparser.PythonGenerator
import kotlin.test.Test
import kotlin.test.assertNotNull

class TestReturn {
    @Test
    fun `test basic return parsing`() {
        val code = """
def test():
    return
        """.trimIndent()
        
        println("Testing code: $code")
        
        val ast = PythonParser.parseWithMetadata(code, emptyList())
        assertNotNull(ast)
        println("AST: $ast")
        
        val generator = PythonGenerator()
        val generatedCode = generator.generateWithMetadata(ast)
        println("Generated: ${generatedCode.code}")
    }
}
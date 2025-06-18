import org.giraffemail.xcode.pythonparser.PythonParser
import org.giraffemail.xcode.pythonparser.PythonGenerator
import org.giraffemail.xcode.ast.*
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
    
    @Test
    fun `test return node generation`() {
        val returnAst = ReturnNode(value = null)
        val generator = PythonGenerator()
        val generatedCode = generator.visitReturnNode(returnAst)
        println("Generated return code: '$generatedCode'")
        
        // Test parsing just the return statement in a function context
        val functionCode = """
def test():
    $generatedCode
        """.trimIndent()
        
        println("Full function code: $functionCode")
        
        val ast = PythonParser.parseWithMetadata(functionCode, emptyList())
        assertNotNull(ast)
        println("Parsed AST: $ast")
    }
}
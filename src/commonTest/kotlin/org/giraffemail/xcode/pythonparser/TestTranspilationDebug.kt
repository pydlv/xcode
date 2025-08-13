import org.giraffemail.xcode.pythonparser.PythonParser
import org.giraffemail.xcode.pythonparser.PythonGenerator
import org.giraffemail.xcode.ast.*
import kotlin.test.Test
import kotlin.test.assertNotNull

class TestTranspilationDebug {
    @Test
    fun `test exact transpilation case`() {
        // This is the exact AST from the failing test
        val functionWithReturnAst = ModuleNode(
            body = listOf(
                FunctionDefNode(
                    name = "test_return",
                    args = emptyList(),
                    body = listOf(
                        ReturnNode(value = null)
                    ),
                    decoratorList = emptyList()
                )
            )
        )

        println("Testing AST: $functionWithReturnAst")

        val generator = PythonGenerator()
        val codeWithMetadata = generator.generateWithNativeMetadata(functionWithReturnAst)
        
        println("Generated code: '${codeWithMetadata.code}'")
        println("Generated metadata: ${codeWithMetadata.metadata}")
        
        // Now try to parse it back
        val ast = PythonParser.parseWithNativeMetadata(codeWithMetadata.code, codeWithMetadata.metadata)
        assertNotNull(ast)
        println("Parsed AST: $ast")
    }
}
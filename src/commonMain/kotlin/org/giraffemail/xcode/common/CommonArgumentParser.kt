package org.giraffemail.xcode.common

import org.giraffemail.xcode.ast.AstParseException
import org.giraffemail.xcode.ast.BinaryOpNode
import org.giraffemail.xcode.ast.ConstantNode
import org.giraffemail.xcode.ast.ExpressionNode

internal object CommonArgumentParser {

    // Regex to parse a simple binary operation like "1 + 2" (very basic)
    private val simpleAdditionRegex = Regex("""(\d+)\s*\+\s*(\d+)""")
    // Regex to parse a simple string literal like "'text'"
    private val stringLiteralRegex = Regex("""\'([^\']*)\'""")

    /**
     * Parses a common argument string that can be a simple string literal or a simple addition.
     * @param argString The string content of the argument.
     * @param contextName A descriptive name of the context for error messages (e.g., "print argument", "console.log argument").
     * @return The parsed ExpressionNode.
     * @throws AstParseException if the argument format is not recognized.
     */
    fun parseCommonExpressionArgument(argString: String, contextName: String): ExpressionNode {
        simpleAdditionRegex.matchEntire(argString)?.let { matchResult ->
            val leftVal = matchResult.groupValues[1].toIntOrNull()
            val rightVal = matchResult.groupValues[2].toIntOrNull()
            if (leftVal != null && rightVal != null) {
                return BinaryOpNode(ConstantNode(leftVal), "+", ConstantNode(rightVal))
            }
        }
        stringLiteralRegex.matchEntire(argString)?.let { matchResult ->
            val strContent = matchResult.groupValues[1]
            return ConstantNode(strContent)
        }
        // Fallback or error for unrecognised argument format
        throw AstParseException("Unsupported $contextName format: $argString")
    }
}


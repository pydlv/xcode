package org.giraffemail.xcode

import org.giraffemail.xcode.ast.AstNode

// This class demonstrates code quality issues that Qodana should detect
class CodeQualityDemo {
    
    // Unused variable - should be detected by static analysis
    private val unusedVariable = "This variable is never used"
    
    // Public field without getter/setter - code smell
    var publicField: String = "This should be private"
    
    // Method with too many parameters - code smell
    fun methodWithTooManyParameters(
        param1: String,
        param2: String,
        param3: String,
        param4: String,
        param5: String,
        param6: String,
        param7: String
    ): String {
        // Multiple unused parameters
        return param1
    }
    
    // Deprecated function usage that should trigger warning
    @Deprecated("This function is deprecated")
    fun deprecatedFunction(): String {
        return "deprecated"
    }
    
    // Function calling deprecated function
    fun callsDeprecatedFunction(): String {
        return deprecatedFunction() // Should warn about calling deprecated function
    }
    
    // Null safety issue - potential null pointer
    fun nullSafetyIssue(input: String?): Int {
        return input?.length ?: 0 // Fixed: using safe call operator
    }
    
    // Unused import and unused function parameter
    fun unusedParameterFunction(usedParam: String, unusedParam: String): String {
        return usedParam
    }
    
    // Magic numbers without constants
    fun magicNumbers(): Double {
        return 3.14159 * 2.71828 // Should suggest using constants
    }
    
    // Empty catch block - bad practice
    fun emptyCatchBlock() {
        try {
            throw RuntimeException("Test")
        } catch (e: Exception) {
            // Empty catch block - should be flagged
        }
    }
    
    // Redundant null check
    fun redundantNullCheck(input: String): String {
        if (input != null) { // Redundant - input is non-null
            return input
        }
        return ""
    }
}
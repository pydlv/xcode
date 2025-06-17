package org.giraffemail.xcode

/**
 * Demo class to test Qodana CLI code quality analysis.
 * This file contains intentional code quality issues.
 */
class CodeQualityTestDemo {
    
    // Public field - should be private with getter/setter
    var publicField: String = "bad practice"
    
    // Unused private field
    private val unusedField = "never used"
    
    // Method with too many parameters
    fun tooManyParameters(a: String, b: String, c: String, d: String, e: String, f: String): String {
        return a + b + c + d + e + f
    }
    
    // Method with magic numbers
    fun magicNumbers(): Int {
        return 42 * 100 + 256  // Magic numbers without constants
    }
    
    // Empty catch block
    fun emptyCatch() {
        try {
            riskyOperation()
        } catch (e: Exception) {
            // Empty catch block - bad practice
        }
    }
    
    // Redundant null check
    fun redundantNullCheck(value: String): Int {
        if (value != null) {  // Always true for non-nullable String
            return value.length
        }
        return 0
    }
    
    // Deprecated function
    @Deprecated("This function is deprecated")
    fun deprecatedFunction(): String {
        return "deprecated"
    }
    
    // Using deprecated function
    fun usingDeprecatedFunction(): String {
        return deprecatedFunction()  // Should warn about using deprecated
    }
    
    private fun riskyOperation() {
        throw RuntimeException("Risk!")
    }
}
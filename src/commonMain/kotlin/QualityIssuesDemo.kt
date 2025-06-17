package org.giraffemail.xcode

import kotlin.collections.List

/**
 * Demo class showing various code quality issues that Qodana should detect
 */
class QualityIssuesDemo {
    
    // Issue: Unused property
    private val unusedProperty = "This is never used"
    
    // Issue: Magic number
    fun calculateWithMagicNumber(value: Int): Int {
        return value * 42  // Magic number should be a constant
    }
    
    // Issue: Nullable safety problem
    fun riskyNullOperation(input: String?): String {
        return input!!.uppercase()  // Dangerous force unwrap
    }
    
    // Issue: Empty catch block
    fun poorErrorHandling() {
        try {
            throw RuntimeException("Test exception")
        } catch (e: Exception) {
            // Empty catch block - bad practice
        }
    }
    
    // Issue: Unused parameter
    fun methodWithUnusedParameter(used: String, unused: String): String {
        return used.lowercase()
    }
    
    // Issue: Complex method that should be simplified
    fun complexMethod(a: Int, b: Int, c: Int, d: Int, e: Int): String {
        var result = ""
        if (a > 0) {
            if (b > 0) {
                if (c > 0) {
                    if (d > 0) {
                        if (e > 0) {
                            result = "All positive"
                        } else {
                            result = "e is not positive"
                        }
                    } else {
                        result = "d is not positive"
                    }
                } else {
                    result = "c is not positive"
                }
            } else {
                result = "b is not positive"
            }
        } else {
            result = "a is not positive"
        }
        return result
    }
    
    // Issue: Redundant null check
    fun redundantNullCheck(value: String): Int {
        if (value != null) {  // Redundant since value is non-null
            return value.length
        }
        return 0
    }
    
    // Issue: Inefficient string concatenation
    fun inefficientStringBuilding(items: List<String>): String {
        var result = ""
        for (item in items) {
            result = result + item + ", "  // Should use StringBuilder
        }
        return result
    }
    
    // Issue: Inconsistent naming (should be camelCase)
    fun method_with_underscore_naming(): String {
        return "Bad naming convention"
    }
    
    // Issue: Unused function
    private fun neverCalledFunction(): String {
        return "This function is never called"
    }
    
    // Issue: Boolean method that could be a property
    fun isAlwaysTrue(): Boolean {
        return true  // This could just be a property
    }
}
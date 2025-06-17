package org.giraffemail.xcode;

import java.util.List;
import java.util.ArrayList;

/**
 * Demo class to test Qodana CLI code quality analysis with Java code.
 * This file contains intentional code quality issues.
 */
public class JavaCodeQualityDemo {
    
    // Public field - should be private with getter/setter
    public String publicField = "bad practice";
    
    // Unused private field
    private String unusedField = "never used";
    
    // Method with too many parameters (more than 5)
    public String tooManyParameters(String a, String b, String c, String d, String e, String f, String g) {
        return a + b + c + d + e + f + g;
    }
    
    // Method with magic numbers
    public int magicNumbers() {
        return 42 * 100 + 256;  // Magic numbers without constants
    }
    
    // Empty catch block
    public void emptyCatch() {
        try {
            riskyOperation();
        } catch (Exception e) {
            // Empty catch block - bad practice
        }
    }
    
    // Redundant null check (unnecessary due to context)
    public int redundantNullCheck(String value) {
        if (value != null && value != null) {  // Redundant check
            return value.length();
        }
        return 0;
    }
    
    // Deprecated method
    @Deprecated
    public String deprecatedMethod() {
        return "deprecated";
    }
    
    // Using deprecated method
    public String usingDeprecatedMethod() {
        return deprecatedMethod();  // Should warn about using deprecated
    }
    
    // Unused local variable
    public void unusedVariable() {
        String unused = "this variable is never used";
        System.out.println("Method executed");
    }
    
    // Raw type usage
    public void rawTypeUsage() {
        List list = new ArrayList();  // Should use generics
        list.add("item");
    }
    
    // Missing @Override annotation
    public String toString() {  // Should have @Override
        return "JavaCodeQualityDemo";
    }
    
    private void riskyOperation() throws Exception {
        throw new Exception("Risk!");
    }
}
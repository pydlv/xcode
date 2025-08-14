/**
 * This is a simple Java program to demonstrate some basic language features.
 * In Java, everything lives inside a class.
 */
public class BasicJavaDemo {

    /**
     * The main method is the entry point of any Java application.
     * The program starts executing from here.
     * 'public' means it can be accessed from anywhere.
     * 'static' means it belongs to the class, not an instance of the class.
     * 'void' means it doesn't return any value.
     * 'String[] args' is an array of strings that can hold command-line arguments.
     */
    public static void main(String[] args) {
        // --- 1. VARIABLES & DATA TYPES ---
        // Variables are containers for storing data values.

        // String: for text
        String greeting = "Hello, Mike! Welcome to Java.";
        System.out.println(greeting);

        // int: for whole numbers
        int myAge = 25;
        System.out.println("Your age is: " + myAge);

        // double: for floating-point numbers (decimals)
        double pi = 3.14159;
        System.out.println("The value of Pi is approximately: " + pi);

        // boolean: for true/false values
        boolean isJavaFun = true;
        System.out.println("Is Java fun? " + isJavaFun);

        System.out.println("\n--- 2. CONTROL FLOW: IF-ELSE STATEMENT ---\n");
        // 'if-else' statements execute different blocks of code based on a condition.
        if (myAge >= 21) {
            System.out.println("You are old enough to enjoy the Las Vegas nightlife!");
        } else {
            System.out.println("Not quite old enough for the casinos yet.");
        }

        System.out.println("\n--- 3. CONTROL FLOW: FOR LOOP ---\n");
        // A 'for' loop is used to repeat a block of code a specific number of times.
        System.out.println("Let's count to 5:");
        for (int i = 1; i <= 5; i++) {
            System.out.println("Count: " + i);
        }

        System.out.println("\n--- 4. METHODS ---\n");
        // Methods are blocks of code that perform a specific task.
        // We can call our custom method 'addNumbers' and store its result.
        int number1 = 10;
        int number2 = 20;
        int sum = addNumbers(number1, number2); // Calling the method
        System.out.println("The sum of " + number1 + " and " + number2 + " is: " + sum);
    }

    /**
     * A simple method to add two integers.
     * It takes two 'int' parameters (a and b).
     * It returns the result as an 'int'.
     */
    public static int addNumbers(int a, int b) {
        return a + b;
    }
}
public class BasicJavaDemo {
    public static void main(String[] args) {
        String greeting = "Hello, Mike! Welcome to Java.";
        System.out.println(greeting);
        int myAge = 25;
        System.out.println("Your age is: " + myAge);
        int pi = 3.14159;
        System.out.println("The value of Pi is approximately: " + pi);
        boolean isJavaFun = true;
        System.out.println("Is Java fun? " + isJavaFun);
        System.out.println("\n--- 2. CONTROL FLOW: IF-ELSE STATEMENT ---\n");
        if (myAge >= 21) {
    System.out.println("You are old enough to enjoy the Las Vegas nightlife!");
} else {
    System.out.println("Not quite old enough for the casinos yet.");
}
        System.out.println("\n--- 3. CONTROL FLOW: FOR LOOP ---\n");
        System.out.println("Let\'s count to 5:");
        int i = 1;
        for (Object _while_dummy : new int[]{1}) {
    System.out.println("Count: " + i);
    int i = 1;
}
        System.out.println("\n--- 4. METHODS ---\n");
        int number1 = 10;
        int number2 = 20;
        int sum = addNumbers(number1, number2);
        System.out.println("The sum of " + number1 + " and " + number2 + " is: " + sum);
    }

    public static void addNumbers(Object a, Object b) {
        return a + b;
    }
}
class BasicJavaDemo {
    function main(args): void {
    let greeting: string = 'Hello, Mike! Welcome to Java.';
    console.log(greeting);
    let myAge: number = 25;
    console.log('Your age is: ' + myAge);
    let pi: number = 3.14159;
    console.log('The value of Pi is approximately: ' + pi);
    let isJavaFun = true;
    console.log('Is Java fun? ' + isJavaFun);
    console.log('\n--- 2. CONTROL FLOW: IF-ELSE STATEMENT ---\n');
    if (myAge >= 21) {
    console.log('You are old enough to enjoy the Las Vegas nightlife!');
} else {
    console.log('Not quite old enough for the casinos yet.');
    console.log('\n--- 3. CONTROL FLOW: FOR LOOP ---\n');
    console.log('Let\\'s count to 5:');
    let i: number = 1;
}
    // Unknown node: Unhandled ANTLR node
}

    console.log('Count: ' + i);

    // Unknown node: Unhandled ANTLR node

    console.log('\n--- 4. METHODS ---\n');

    let number1: number = 10;

    let number2: number = 20;

    let sum = addNumbers(number1, number2);

    console.log('The sum of ' + number1 + ' and ' + number2 + ' is: ' + sum);
}
function addNumbers(a, b): void {
    return a + b;
}
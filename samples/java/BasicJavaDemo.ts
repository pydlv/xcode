class BasicJavaDemo {
    function main(args): void {
    let greeting: string = 'Hello, Mike! Welcome to Java.';
    console.log(greeting);
    let myAge: number = 25;
    console.log('Your age is: ' + myAge);
    let pi: number = 3.14159;
    console.log('The value of Pi is approximately: ' + pi);
    let isJavaFun: boolean = true;
    console.log('Is Java fun? ' + isJavaFun);
    console.log('
--- 2. CONTROL FLOW: IF-ELSE STATEMENT ---
');
    if (myAge >= 21) {
    console.log('You are old enough to enjoy the Las Vegas nightlife!');
} else {
    console.log('Not quite old enough for the casinos yet.');
}
    console.log('
--- 3. CONTROL FLOW: FOR LOOP ---
');
    console.log('Let\'s count to 5:');
    let i: number = 1;
    for (let _while_dummy of [1]) {
    console.log('Count: ' + i);
    let i: number = 1;
}
    console.log('
--- 4. METHODS ---
');
    let number1: number = 10;
    let number2: number = 20;
    let sum = addNumbers(number1, number2);
    console.log('The sum of ' + number1 + ' and ' + number2 + ' is: ' + sum);
}

    function addNumbers(a, b): void {
    return a + b;
}
}
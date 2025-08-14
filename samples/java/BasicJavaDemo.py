class BasicJavaDemo:
    def main(args):
        greeting = 'Hello, Mike! Welcome to Java.'
        print(greeting)
        myAge = 25
        print('Your age is: ' + myAge)
        pi = 3.14159
        print('The value of Pi is approximately: ' + pi)
        isJavaFun = true
        print('Is Java fun? ' + isJavaFun)
        print('\n--- 2. CONTROL FLOW: IF-ELSE STATEMENT ---\n')
        if myAge >= 21:
        print('You are old enough to enjoy the Las Vegas nightlife!')
    else:
        print('Not quite old enough for the casinos yet.')
        print('\n--- 3. CONTROL FLOW: FOR LOOP ---\n')
        print('Let\'s count to 5:')
        i = 1
    while i <= 5:
        print('Count: ' + i)
        i += 1
        print('\n--- 4. METHODS ---\n')
        number1 = 10
        number2 = 20
        sum = addNumbers(number1, number2)
        print('The sum of ' + number1 + ' and ' + number2 + ' is: ' + sum)

    def addNumbers(a, b):
        return a + b
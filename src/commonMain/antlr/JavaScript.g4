grammar JavaScript;

program: statement* EOF;

statement: consoleLogStatement
         | functionDeclaration
         ;

consoleLogStatement: 'console' '.' 'log' '(' expression ')' ';'? ;

functionDeclaration:
    'function' IDENTIFIER '(' parameterList? ')' '{' functionBody '}' ;

parameterList: IDENTIFIER (',' IDENTIFIER)* ;

functionBody: statement* ;

expression
    : STRING_LITERAL '+' expression  # StringAddition
    | NUMBER '+' NUMBER               # SimpleAddition
    | STRING_LITERAL                 # StringLiteral
    | IDENTIFIER                     # Identifier
    | NUMBER                         # NumberLiteral
    ;

STRING_LITERAL: '\'' ( '\\' . | ~['\\] )* '\'' | '"' ( '\\' . | ~["\\] )* '"' ;
IDENTIFIER: [a-zA-Z_] [a-zA-Z_0-9]* ;
NUMBER: [0-9]+ ('.' [0-9]+)? ;
WS: [ \t\r\n]+ -> skip ;

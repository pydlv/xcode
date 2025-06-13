grammar JavaScript;

program: statement* EOF;

statement: consoleLogStatement;

consoleLogStatement: 'console' '.' 'log' '(' expression ')' ';'? ;

expression
    : NUMBER '+' NUMBER # SimpleAddition
    | STRING_LITERAL   # StringLiteral
    | IDENTIFIER       # Identifier
    | NUMBER           # NumberLiteral
    ;

STRING_LITERAL: '\'' ( '\\' . | ~['\\] )* '\'' | '"' ( '\\' . | ~["\\] )* '"' ;
IDENTIFIER: [a-zA-Z_] [a-zA-Z_0-9]* ;
NUMBER: [0-9]+ ('.' [0-9]+)? ;
WS: [ \t\r\n]+ -> skip ;

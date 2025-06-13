grammar Python;

program: statement* EOF;

statement: printStatement
        | functionDef
        ;

printStatement: 'print' '(' expression ')' ;

// Simplified function definition for testing purposes
functionDef: 'def' IDENTIFIER '(' parameters? ')' ':' functionBody ;

parameters: parameter (',' parameter)* ;
parameter: IDENTIFIER ;

// Simplified body handling for tests
functionBody: printStatement ;

expression
    : expression '+' expression  # Addition
    | STRING_LITERAL            # StringLiteral
    | IDENTIFIER                # Identifier
    | NUMBER                    # NumberLiteral
    ;

STRING_LITERAL: '\'' ( '\\' . | ~['\\] )* '\'' | '"' ( '\\' . | ~["\\] )* '"' ;
IDENTIFIER: [a-zA-Z_] [a-zA-Z_0-9]* ;
NUMBER: [0-9]+ ('.' [0-9]+)? ;
WHITESPACE: [ \t\r\n]+ -> skip ;

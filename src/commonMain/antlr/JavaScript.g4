grammar JavaScript;

program: statement* EOF;

statement
    : consoleLogStatement
    | functionDeclaration
    | assignStatement
    | functionCallStatement
    ;

consoleLogStatement: 'console' '.' 'log' '(' expression ')' ';'? ;

functionDeclaration:
    'function' IDENTIFIER '(' parameterList? ')' '{' functionBody '}' ;

parameterList: IDENTIFIER (',' IDENTIFIER)* ;

functionBody: statement* ;

assignStatement: 'let'? IDENTIFIER '=' expression ';'? ;

functionCallStatement: IDENTIFIER '(' arguments? ')' ';'? ;

arguments: expression (',' expression)* ;

expression
    : expression '+' expression  # Addition
    | IDENTIFIER '(' arguments? ')'  # FunctionCall
    | STRING_LITERAL            # StringLiteral
    | IDENTIFIER                # Identifier
    | NUMBER                    # NumberLiteral
    ;

STRING_LITERAL: '\'' ( '\\' . | ~['\\] )* '\'' | '"' ( '\\' . | ~["\\] )* '"' ;
IDENTIFIER: [a-zA-Z_] [a-zA-Z_0-9]* ;
NUMBER: [0-9]+ ('.' [0-9]+)? ;
WS: [ \t\r\n]+ -> skip ;

grammar Java;

// Parser Rules
compilationUnit: statement* EOF;

statement: expressionStatement;

expressionStatement: expression ';';

expression:
      primary                                         # PrimaryExpression
    | expression ADD expression                  # AdditiveExpression
    | SYSTEM '.' OUT '.' PRINTLN LPAREN expression RPAREN # PrintlnExpression
    ;

primary:
    literal                                         # LiteralExpression
    ;

literal:
    STRING_LITERAL
    |   DECIMAL_LITERAL
    ;

// Lexer Rules
SYSTEM: 'System';
OUT: 'out';
PRINTLN: 'println';

STRING_LITERAL: '"' (~["\r\n] | '\\"')* '"';
DECIMAL_LITERAL: [0-9]+ ('.' [0-9]+)?; // Allows for integers and simple decimals

LPAREN: '(';
RPAREN: ')';
SEMI: ';';
DOT: '.';

ADD : '+';

WS: [ \t\r\n]+ -> skip;
COMMENT: '//' .*? '\r'? '\n' -> skip;
LINE_COMMENT: '/*' .*? '*/' -> skip;

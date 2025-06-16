grammar Java;

import CommonLexerRules; // Import common lexer rules

// Parser Rules
compilationUnit: statement* EOF;

statement: expressionStatement;

expressionStatement: expression SEMI; // Changed from ';' to SEMI

expression:
      primary                                         # PrimaryExpression
    | expression ADD expression                  # AdditiveExpression
    | SYSTEM '.' OUT '.' PRINTLN LPAREN expression RPAREN # PrintlnExpression
    ;

primary:
    literal                                         # LiteralExpression
    ;

literal:
    STRING_LITERAL // Now uses common STRING_LITERAL
    |   NUMBER         // Now uses common NUMBER (was DECIMAL_LITERAL)
    ;

SYSTEM: 'System';
OUT: 'out';
PRINTLN: 'println';

LPAREN: '(';
RPAREN: ')';
SEMI: ';';
DOT: '.';

ADD : '+';

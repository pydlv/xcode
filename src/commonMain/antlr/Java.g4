grammar Java;

import CommonLexerRules; // Import common lexer rules

// Parser Rules
compilationUnit: statement* EOF;

statement:
      functionDefinition
    | callStatement
    | assignmentStatement
    | expressionStatement
    ;

expressionStatement: expression SEMI; // Changed from ';' to SEMI

// Function Definition
functionDefinition:
    PUBLIC STATIC VOID IDENTIFIER LPAREN parameterList RPAREN LBRACE statement* RBRACE
    ;

parameterList:
    (parameter (COMMA parameter)*)?
    ;

parameter:
    IDENTIFIER IDENTIFIER // Represents type and name, e.g., "Object a"
    ;

    // Assignment Statement
assignmentStatement:
    IDENTIFIER ASSIGN expression SEMI
    ;

    // Call Statement (for standalone calls like fib(0,1); or fib(b,c);)
callStatement:
    IDENTIFIER LPAREN argumentList RPAREN SEMI
    ;

argumentList:
    (expression (COMMA expression)*)?
    ;

expression:
      primary                                         # PrimaryExpression
    | expression ADD expression                       # AdditiveExpression
    | SYSTEM '.' OUT '.' PRINTLN LPAREN expression RPAREN # PrintlnExpression
    | IDENTIFIER                                      # IdentifierAccessExpression // Added for variables like a, b, c
    | IDENTIFIER LPAREN argumentList RPAREN           # CallExpression // Added for calls within expressions (if needed, though callStatement covers current use)
    ;

primary:
    literal                                         # LiteralExpression
    | IDENTIFIER                                    # IdentifierPrimary // Added to allow identifiers as primary expressions
    | LPAREN expression RPAREN                      # ParenthesizedExpression
    ;

literal:
    STRING_LITERAL // Now uses common STRING_LITERAL
    |   NUMBER         // Now uses common NUMBER (was DECIMAL_LITERAL)
    ;

    // Keywords and Operators (Lexer Rules)
PUBLIC: 'public';
STATIC: 'static';
VOID: 'void';
// INT: 'int'; // Example, not strictly needed if types are treated as IDENTIFIER

SYSTEM: 'System';
OUT: 'out';
PRINTLN: 'println';

LPAREN: '(';
RPAREN: ')';
LBRACE: '{'; // Added
RBRACE: '}'; // Added
SEMI: ';';
DOT: '.';
COMMA: ',';   // Added
ASSIGN: '=';  // Added

ADD : '+';

// IDENTIFIER, NUMBER, STRING_LITERAL are expected to be in CommonLexerRules.g4
// Whitespace and comments might also be in CommonLexerRules.g4 or need to be defined.
// For simplicity, assuming IDENTIFIER is defined in CommonLexerRules as [a-zA-Z_][a-zA-Z_0-9]*
// and whitespace is handled (e.g., skipped).
WS: [ \\t\\r\\n]+ -> skip; // Common whitespace skipping rule, if not in CommonLexerRules

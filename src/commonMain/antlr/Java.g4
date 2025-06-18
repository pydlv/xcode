grammar Java;

import CommonLexerRules; // Import common lexer rules

// Parser Rules
compilationUnit: statement* EOF;

statement:
      functionDefinition
    | callStatement
    | assignmentStatement
    | expressionStatement
    | ifStatement
    | returnStatement
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

    // If Statement
ifStatement:
    IF LPAREN expression RPAREN LBRACE statement* RBRACE (ELSE LBRACE statement* RBRACE)?
    ;

    // Return Statement
returnStatement:
    RETURN expression? SEMI // Return statement with optional expression
    ;

argumentList:
    (expression (COMMA expression)*)?
    ;

expression:
      primary                                         # PrimaryExpression
    | expression ADD expression                       # AdditiveExpression
    | expression ('==' | '!=' | '<' | '>' | '<=' | '>=') expression # ComparisonExpression
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
IF: 'if';
ELSE: 'else';
RETURN: 'return'; // Added return keyword
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
// Whitespace and comments are defined in CommonLexerRules.g4

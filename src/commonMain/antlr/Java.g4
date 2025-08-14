grammar Java;

import CommonLexerRules; // Import common lexer rules

// Parser Rules
compilationUnit: statement* EOF;

statement:
      functionDefinition
    | classDefinition
    | callStatement
    | assignmentStatement
    | expressionStatement
    | ifStatement
    | forStatement
    | returnStatement
    ;

expressionStatement: expression SEMI; // Changed from ';' to SEMI

// Function Definition - now supports return types
functionDefinition:
    PUBLIC STATIC returnType IDENTIFIER LPAREN parameterList RPAREN LBRACE statement* RBRACE
    ;

returnType:
    VOID | type
    ;

// Class Definition  
classDefinition:
    PUBLIC CLASS IDENTIFIER (EXTENDS IDENTIFIER)? LBRACE classMember* RBRACE
    ;

classMember:
    functionDefinition
    | statement
    ;

parameterList:
    (parameter (COMMA parameter)*)?
    ;

parameter:
    type IDENTIFIER // Represents type and name, e.g., "String[] args", "int x"
    ;

    // Assignment Statement - supports both typed and untyped
assignmentStatement:
    (type IDENTIFIER ASSIGN expression | IDENTIFIER ASSIGN expression) SEMI
    ;

// Type declaration - supports arrays
type:
    IDENTIFIER (LBRACKET RBRACKET)*  // e.g., String, String[], Object[][]
    | primitiveType (LBRACKET RBRACKET)*  // e.g., int, double, boolean, int[]
    ;

primitiveType:
    'int' | 'double' | 'float' | 'boolean' | 'char' | 'byte' | 'short' | 'long'
    ;

    // Call Statement (for standalone calls like fib(0,1); or fib(b,c);)
callStatement:
    IDENTIFIER LPAREN argumentList RPAREN SEMI
    ;

    // If Statement
ifStatement:
    IF LPAREN expression RPAREN LBRACE statement* RBRACE (ELSE LBRACE statement* RBRACE)?
    ;

// Traditional C-style for loop (for (int i = 1; i <= 5; i++))
forStatement:
    FOR LPAREN forInit SEMI expression SEMI forUpdate RPAREN LBRACE statement* RBRACE
    ;

forInit:
    (type IDENTIFIER ASSIGN expression | IDENTIFIER ASSIGN expression)?
    ;

forUpdate:
    (IDENTIFIER INCR | IDENTIFIER DECR | assignmentExpression)?
    ;

assignmentExpression:
    IDENTIFIER ASSIGN expression
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
    | arrayInitializer                              # ArrayInitializerExpression
    | IDENTIFIER                                    # IdentifierPrimary // Added to allow identifiers as primary expressions
    | LPAREN expression RPAREN                      # ParenthesizedExpression
    ;

arrayInitializer:
    'new' type LBRACKET RBRACKET LBRACE arrayElements? RBRACE      # ArrayInit
    ;

arrayElements:
    expression (',' expression)*
    ;

literal:
    STRING_LITERAL // Now uses common STRING_LITERAL
    |   NUMBER         // Now uses common NUMBER (was DECIMAL_LITERAL)
    ;

    // Keywords and Operators (Lexer Rules)
PUBLIC: 'public';
STATIC: 'static';
VOID: 'void';
CLASS: 'class';
EXTENDS: 'extends';
IF: 'if';
ELSE: 'else';
FOR: 'for';
RETURN: 'return'; // Added return keyword
// INT: 'int'; // Example, not strictly needed if types are treated as IDENTIFIER

SYSTEM: 'System';
OUT: 'out';
PRINTLN: 'println';

LPAREN: '(';
RPAREN: ')';
LBRACE: '{'; // Added
RBRACE: '}'; // Added
LBRACKET: '['; // Added for arrays
RBRACKET: ']'; // Added for arrays
SEMI: ';';
DOT: '.';
COMMA: ',';   // Added
ASSIGN: '=';  // Added
INCR: '++';   // Added for i++
DECR: '--';   // Added for i--
COLON: ':';   // Added for for loops

ADD : '+';

// IDENTIFIER, NUMBER, STRING_LITERAL are expected to be in CommonLexerRules.g4
// Whitespace and comments are defined in CommonLexerRules.g4

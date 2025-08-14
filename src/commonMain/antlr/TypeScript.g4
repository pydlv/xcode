grammar TypeScript;

import CommonLexerRules; // Import common lexer rules

program: statement* EOF;

statement
    : consoleLogStatement
    | functionDeclaration
    | classDeclaration
    | assignStatement
    | functionCallStatement
    | ifStatement
    | forStatement
    | returnStatement
    ;

consoleLogStatement: 'console' '.' 'log' '(' expression ')' ';'? ; // Using literal chars

functionDeclaration:
    'function' IDENTIFIER '(' parameterList? ')' typeAnnotation? '{' functionBody '}' ; // Using literal chars

classDeclaration:
    'class' IDENTIFIER ('extends' IDENTIFIER)? '{' classBody '}' ; // Class with optional inheritance

classBody: classMember* ;

classMember:
    functionDeclaration
    | assignStatement
    ;

parameterList: parameter (',' parameter)* ; // Using literal chars

parameter: IDENTIFIER typeAnnotation? ; // TypeScript parameter with optional type annotation

typeAnnotation: ':' typeExpression ; // TypeScript type annotation

typeExpression: 
    'string' | 'number' | 'boolean' | 'void' | 'any' 
    | typeExpression '[' ']' // Array type like string[] or number[]
    | '[' tupleTypeList ']' // Tuple type like [string, number]
    | IDENTIFIER ; // Basic TypeScript types

tupleTypeList: typeExpression (',' typeExpression)* ; // List of types in a tuple

functionBody: statement* ;

assignStatement: ('let' | 'var' | 'const')? IDENTIFIER typeAnnotation? '=' expression ';'? ; // Using literal chars

functionCallStatement: IDENTIFIER '(' arguments? ')' ';'? ; // Using literal chars

ifStatement: 'if' '(' expression ')' '{' functionBody '}' ('else' '{' functionBody '}')? ; // Using literal chars

forStatement: 'for' '(' ('let' | 'var' | 'const')? IDENTIFIER typeAnnotation? 'of' expression ')' '{' functionBody '}' ; // TypeScript for-of loop with optional type annotation

returnStatement: 'return' expression? ';'? ; // Return statement with optional expression and semicolon

arguments: expression (',' expression)* ; // Using literal chars

expression
    : expression '+' expression  # Addition
    | expression ('===' | '!==' | '==' | '!=' | '<' | '>' | '<=' | '>=') expression # Comparison
    | IDENTIFIER '(' arguments? ')'  # FunctionCall
    | '[' arrayElements? ']'   # ArrayLiteral  // Array literal like [1, 2, 3]
    | STRING_LITERAL            # StringLiteral // Uses common STRING_LITERAL
    | IDENTIFIER                # Identifier    // Uses common IDENTIFIER
    | NUMBER                    # NumberLiteral // Uses common NUMBER
    ;

arrayElements: expression (',' expression)* ; // List of elements in an array

// Lexer Rules - Most are now imported
// STRING_LITERAL, IDENTIFIER, NUMBER are now imported from CommonLexerRules.
// WS is now handled by WS_ALL from CommonLexerRules.
// TypeScript comments (// and /* */) are handled by SL_COMMENT and ML_COMMENT from CommonLexerRules.
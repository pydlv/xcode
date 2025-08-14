grammar JavaScript;

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
    'function' IDENTIFIER '(' parameterList? ')' '{' functionBody '}' ; // Using literal chars

classDeclaration:
    'class' IDENTIFIER ('extends' IDENTIFIER)? '{' classBody '}' ; // Class with optional inheritance

classBody: classMember* ;

classMember:
    functionDeclaration
    | assignStatement
    ;

parameterList: IDENTIFIER (',' IDENTIFIER)* ; // Using literal chars

functionBody: statement* ;

assignStatement: 'let'? IDENTIFIER '=' expression ';'? ; // Using literal chars

functionCallStatement: IDENTIFIER '(' arguments? ')' ';'? ; // Using literal chars

ifStatement: 'if' '(' expression ')' '{' functionBody '}' ('else' '{' functionBody '}')? ; // Using literal chars

forStatement: 'for' '(' 'let' IDENTIFIER 'of' expression ')' '{' functionBody '}' ; // for-of loop only (no else clause)

returnStatement: 'return' expression? ';'? ; // Return statement with optional expression and semicolon

arguments: expression (',' expression)* ; // Using literal chars

expression
    : expression '+' expression  # Addition
    | expression ('===' | '!==' | '==' | '!=' | '<' | '>' | '<=' | '>=') expression # Comparison
    | IDENTIFIER '(' arguments? ')'  # FunctionCall
    | '[' arrayElements? ']'   # ArrayLiteral  // Array literal support
    | STRING_LITERAL            # StringLiteral // Uses common STRING_LITERAL
    | IDENTIFIER                # Identifier    // Uses common IDENTIFIER
    | NUMBER                    # NumberLiteral // Uses common NUMBER
    ;

arrayElements: expression (',' expression)* ;

// Lexer Rules - Most are now imported
// STRING_LITERAL, IDENTIFIER, NUMBER are now imported from CommonLexerRules.
// WS is now handled by WS_ALL from CommonLexerRules.
// JavaScript comments (// and /* */) are handled by SL_COMMENT and ML_COMMENT from CommonLexerRules.

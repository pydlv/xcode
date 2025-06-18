grammar Python;

import CommonLexerRules; // Import common lexer rules

program: program_body? EOF;

program_body // Sequence of top-level statements
    : topLevelStatement (NEWLINE+ topLevelStatement)* NEWLINE*
    ;

topLevelStatement
    : functionDef
    | classDef
    | statement
    ;

statement // Basic statements
    : printStatement
    | assignStatement
    | functionCallStatement
    | ifStatement
    | returnStatement
    ;

printStatement: 'print' '(' expression ')' ; // Using literal parens

functionDef: 'def' IDENTIFIER '(' parameters? ')' ':' NEWLINE INDENT function_body? DEDENT ; // function_body is now optional

classDef: 'class' IDENTIFIER ('(' baseClasses? ')')? ':' NEWLINE INDENT class_body? DEDENT ; // class definition with optional inheritance and body

class_body // Sequence of statements within a class - similar to function_body
    : NEWLINE* classMember (NEWLINE+ classMember)* NEWLINE*
    ;

classMember // Class members can be methods or other statements
    : functionDef
    | statement
    ;

baseClasses: IDENTIFIER (',' IDENTIFIER)* ; // For inheritance like class A(B, C)

function_body // Sequence of statements within a function - MODIFIED
    : NEWLINE* statement (NEWLINE+ statement)* NEWLINE*
    ;

parameters: parameter (',' parameter)* ;
parameter: IDENTIFIER ;

assignStatement: IDENTIFIER '=' expression ';'? ; // Optional semicolon for cross-language compatibility

functionCallStatement: IDENTIFIER '(' arguments? ')' ';'? ; // Optional semicolon for cross-language compatibility

ifStatement: 'if' expression ':' NEWLINE INDENT function_body? DEDENT (NEWLINE* 'else' ':' NEWLINE INDENT function_body? DEDENT)? ;

returnStatement: 'return' expression? ';'? ; // Optional expression and optional semicolon for cross-language compatibility

arguments: expression (',' expression)* ;

expression
    : expression '+' expression          # Addition
    | expression ('==' | '!=' | '<' | '>' | '<=' | '>=') expression # Comparison
    | IDENTIFIER '(' arguments? ')'      # FunctionCallInExpression // Using literal parens
    | '[' expressionList? ']'            # ListLiteral  // Python list literals like [1, 2, 3]
    | ('True' | 'False')                 # BooleanLiteral // Python boolean literals
    | STRING_LITERAL                     # StringLiteral // Uses common STRING_LITERAL
    | IDENTIFIER                         # Identifier    // Uses common IDENTIFIER
    | NUMBER                             # NumberLiteral // Uses common NUMBER
    ;

expressionList: expression (',' expression)* ; // List of expressions for lists

// Lexer Rules - Most are now imported
// Keywords/Special Tokens first - order matters for tokens that could also be identifiers
INDENT: 'INDENT' ; // Specific to Python, keep here
DEDENT: 'DEDENT' ; // Specific to Python, keep here

// STRING_LITERAL, IDENTIFIER, NUMBER are now imported from CommonLexerRules

// Whitespace and Newlines
NEWLINE: ( '\r'? '\n' | '\r' )+ ; // Python specific newline handling, keep here
// WHITESPACE is now WS_HORIZONTAL from CommonLexerRules
// Python does not use // or /* */ comments by default, so SL_COMMENT and ML_COMMENT are not used.
// If Python needs to support # comments, a rule like: HASH_COMMENT: '#' ~[\r\n]* -> skip; would be added here.

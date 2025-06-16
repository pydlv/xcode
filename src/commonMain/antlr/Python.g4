grammar Python;

import CommonLexerRules; // Import common lexer rules

program: program_body? EOF;

program_body // Sequence of top-level statements
    : topLevelStatement (NEWLINE+ topLevelStatement)* NEWLINE*
    ;

topLevelStatement
    : functionDef
    | statement
    ;

statement // Basic statements
    : printStatement
    | assignStatement
    | functionCallStatement
    | ifStatement
    ;

printStatement: 'print' '(' expression ')' ; // Using literal parens

functionDef: 'def' IDENTIFIER '(' parameters? ')' ':' NEWLINE INDENT function_body? DEDENT ; // function_body is now optional

function_body // Sequence of statements within a function - MODIFIED
    : NEWLINE* statement (NEWLINE+ statement)* NEWLINE*
    ;

parameters: parameter (',' parameter)* ;
parameter: IDENTIFIER ;

assignStatement: IDENTIFIER '=' expression ;

functionCallStatement: IDENTIFIER '(' arguments? ')' ; // Using literal parens

ifStatement: 'if' expression ':' NEWLINE INDENT function_body? DEDENT (NEWLINE* 'else' ':' NEWLINE INDENT function_body? DEDENT)? ;

arguments: expression (',' expression)* ;

expression
    : expression '+' expression          # Addition
    | expression ('==' | '!=' | '<' | '>' | '<=' | '>=') expression # Comparison
    | IDENTIFIER '(' arguments? ')'      # FunctionCallInExpression // Using literal parens
    | STRING_LITERAL                     # StringLiteral // Uses common STRING_LITERAL
    | IDENTIFIER                         # Identifier    // Uses common IDENTIFIER
    | NUMBER                             # NumberLiteral // Uses common NUMBER
    ;

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

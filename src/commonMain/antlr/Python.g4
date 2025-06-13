grammar Python;

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
    ;

printStatement: 'print' '(' expression ')' ;

functionDef: 'def' IDENTIFIER '(' parameters? ')' ':' NEWLINE INDENT function_body? DEDENT ; // function_body is now optional

function_body // Sequence of statements within a function - MODIFIED
    : NEWLINE* statement (NEWLINE+ statement)* NEWLINE*
    ;

parameters: parameter (',' parameter)* ;
parameter: IDENTIFIER ;

assignStatement: IDENTIFIER '=' expression ;

functionCallStatement: IDENTIFIER '(' arguments? ')' ;

arguments: expression (',' expression)* ;

expression
    : expression '+' expression          # Addition
    | IDENTIFIER '(' arguments? ')'      # FunctionCallInExpression
    | STRING_LITERAL                     # StringLiteral
    | IDENTIFIER                         # Identifier
    | NUMBER                             # NumberLiteral
    ;

// Lexer Rules
// Keywords/Special Tokens first - order matters for tokens that could also be identifiers
INDENT: 'INDENT' ;
DEDENT: 'DEDENT' ;

// Literals and Identifiers
STRING_LITERAL: '\'' ( '\\' . | ~['\\] )* '\'' | '"' ( '\\' . | ~["\\] )* '"' ;
IDENTIFIER: [a-zA-Z_] [a-zA-Z_0-9]* ; // Must be after INDENT/DEDENT and other keywords defined as lexer rules
NUMBER: [0-9]+ ('.' [0-9]+)? ;

// Whitespace and Newlines
NEWLINE: ( '\r'? '\n' | '\r' )+ ; // Match one or more newlines/carriage returns. Consolidates multiple blank lines.
WHITESPACE: [ \t]+ -> skip ; // Skips whitespace characters except newlines (handled by NEWLINE token)

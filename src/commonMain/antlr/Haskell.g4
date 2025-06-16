grammar Haskell;

import CommonLexerRules; // Import common lexer rules

program: topLevelDeclaration* EOF;

topLevelDeclaration
    : functionDefinition
    | statement
    ;

statement
    : printStatement
    | assignment
    | functionCallStatement  
    | ifStatement
    ;

// print "text" or putStrLn "text"
printStatement: ('print' | 'putStrLn') expression ;

// func arg1 arg2 = body
functionDefinition: IDENTIFIER parameterList? '=' expression ;

parameterList: IDENTIFIER+ ;

// variable = expression
assignment: IDENTIFIER '=' expression ;

// func arg1 arg2
functionCallStatement: IDENTIFIER arguments? ;

// if condition then expr else expr
ifStatement: 'if' expression 'then' expression 'else' expression ;

arguments: expression+ ;

expression
    : '(' expression ')'                 # ParenthesizedExpression
    | IDENTIFIER arguments               # FunctionCallWithArgs  
    | expression '+' expression          # Addition
    | expression ('==' | '/=' | '<' | '>' | '<=' | '>=') expression # Comparison
    | STRING_LITERAL                     # StringLiteral
    | IDENTIFIER                         # Identifier
    | NUMBER                            # NumberLiteral
    ;

// Additional Haskell-specific lexer rules
HASKELL_COMMENT: '--' ~[\r\n]* -> skip;
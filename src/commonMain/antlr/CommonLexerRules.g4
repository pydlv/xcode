// filepath: c:\Users\pydlv\IdeaProjects\xcode\src\commonMain\antlr\CommonLexerRules.g4
lexer grammar CommonLexerRules;

// A common definition for identifiers.
// Used by Python, JavaScript, and can be adopted by Java.
IDENTIFIER: [a-zA-Z_] [a-zA-Z_0-9]* ;

// A common definition for number literals (integers and decimals).
// Matches DECIMAL_LITERAL in Java and NUMBER in Python/JavaScript.
NUMBER: [0-9]+ ('.' [0-9]+)? ;

// A common definition for string literals, supporting both single and double quotes.
// Matches Python and JavaScript. Java's original rule is for double quotes only
// and has slightly different internal character matching. Adopting this may make
// the Java lexer more permissive regarding quote types if not constrained by parser rules.
STRING_LITERAL: '\'' ( '\\' . | ~['\\] )* '\'' | '"' ( '\\' . | ~["\\] )* '"' ;

// Whitespace rules
WS_ALL: [ \t\r\n]+ -> skip;
WS_HORIZONTAL: [ \t]+ -> skip;

// Comment rules
// Metadata comment rule (not skipped) - must come before SL_COMMENT
METADATA_COMMENT: '//' .*? '__TS_META__:' .*? ;
SL_COMMENT : '//' ~[\r\n]* -> skip;
ML_COMMENT: '/*' .*? '*/' -> skip;

grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LRECT : '[' ;
RRECT : ']' ;
LPAREN : '(' ;
RPAREN : ')' ;
COMMA : ',' ;
MUL : '*' ;
ADD : '+' ;
SUB : '-' ;
DIV : '/' ;
NOT: '!' ;

CLASS : 'class' ;
INT : 'int' ;
BOOLEAN : 'boolean' ;
STRING : 'String' ;
PUBLIC : 'public' ;
STATIC: 'static';
VOID : 'void' ;
RETURN : 'return' ;
BOOL : ('true' | 'false');
ELLIPSIS : '...';

INTEGER : [0-9]+ ;
ID : [a-zA-Z]+ ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDeclaration* classDecl* methodDecl* EOF;


importDeclaration
    : 'import' value+=ID( '.' value+=ID)*';' ;


classDecl
    : CLASS name=ID ('extends' ext=ID)?
        LCURLY
        (varDecl | methodDecl)*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name= (INT|BOOLEAN|ID|VOID|STRING) (LRECT RRECT)* ;

methodDecl locals[boolean isPublic=false, boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})? (STATIC {$isStatic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;
param
    : type ELLIPSIS? name= ID (COMMA type ELLIPSIS? name=ID)*
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | LCURLY (expr SEMI)* RCURLY #BlockStmt //
    | ID LRECT ID RRECT EQUALS expr SEMI #ArrayAssignStmt //
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : LPAREN expr RPAREN #ParenExpr //
    | expr op= MUL expr #BinaryExpr //
    | expr op= DIV expr #BinaryExpr
    | expr op= ADD expr #BinaryExpr //
    | expr op= SUB expr #BinaryExpr
    | op= NOT expr #UnaryExpr
    | ID '.' ID LPAREN (expr (COMMA expr)*)? RPAREN #MethodCallExpr //
    | value=INTEGER #IntegerLiteral //
    | BOOL #BooleanLiteral
    | name=ID #VarRefExpr //
    ;




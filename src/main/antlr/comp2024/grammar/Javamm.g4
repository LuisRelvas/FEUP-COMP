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
ID : [a-zA-Z]+ [a-zA-Z0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDeclaration* classDecl* methodDecl* EOF;


importDeclaration
    : 'import' value+=ID( '.' value+=ID)*';' ;


classDecl
    : CLASS name=ID ('extends' ext=ID)?
        LCURLY
        (varDecl)* (methodDecl)*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : type LRECT RRECT #ArrayType
    | value=INT     #IntType
    | value=BOOLEAN #BooleanType
    | value=ID      #ClassType
    | value=VOID    #VoidType
    | value=STRING  #StringType
    ;

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
    | 'if' LPAREN expr RPAREN (LCURLY stmt* RCURLY | stmt) ('else' (LCURLY stmt* RCURLY | stmt))? #IfStmt //
    | 'while' LPAREN expr RPAREN (LCURLY stmt* RCURLY | stmt) #WhileStmt //
    | ID SEMI #GeneralStmt
    ;

expr
    : LPAREN expr RPAREN #ParenExpr //
    | 'new' type ((LRECT expr RRECT)|(LPAREN expr* RPAREN)) #NewArrayExpr //
    | expr op= MUL expr #BinaryExpr //
    | expr op= DIV expr #BinaryExpr
    | expr op= ADD expr #BinaryExpr //
    | expr op= SUB expr #BinaryExpr
    | op= NOT expr #UnaryExpr
    | expr op= '<' expr #BinaryExpr //
    | expr op= '>' expr #BinaryExpr //
    | expr op= '<=' expr #BinaryExpr //
    | expr op= '>=' expr #BinaryExpr //
    | expr op= '==' expr #BinaryExpr //
    | expr op= '!=' expr #BinaryExpr //
    | expr op= '&&' expr #BinaryExpr //
    | expr op= '||' expr #BinaryExpr //
    | LRECT (expr (COMMA expr)*)? RRECT #ArrayInitExpr //
    | expr (LRECT expr RRECT)+  #ArrayAccessExpr //
    |  ID ('.' ID LPAREN (expr (COMMA expr)*)? RPAREN)* #MethodCallExpr //
    | ID (('.' ID LPAREN (expr (COMMA expr)*)? RPAREN) | ('[' expr ']') | ('.' 'length'))* #ChainedExpr
    | ID ('.' 'length')* #LengthExpr //
    | value=INTEGER #IntegerLiteral //
    | BOOL #BooleanLiteral
    | name=ID #VarRefExpr //
    ;




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
    : (importDeclaration)* classDecl EOF;


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

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})? type name=ID LPAREN (type name=ID (COMMA type name=ID)*)? RPAREN LCURLY (varDecl)* (stmt)* 'return' expr SEMI RCURLY
    | (PUBLIC)? STATIC VOID 'main' LPAREN STRING LRECT RRECT name=ID RPAREN LCURLY (varDecl)* (stmt)* RCURLY
    ;


type
    : INT LRECT RRECT #ArrayType
    | INT ELLIPSIS #ArrayType
    | value=INT     #IntType
    | value=BOOLEAN #BooleanType
    | value=ID      #ClassType
    | value=STRING  #StringType
    ;


param
    : type ELLIPSIS? name= ID (COMMA type ELLIPSIS? name=ID)*
    ;

stmt
    : LCURLY (stmt)* RCURLY #BlockStmt //
    | 'if' LPAREN expr RPAREN stmt 'else' stmt #IfStmt //
    | 'while' LPAREN expr RPAREN stmt #WhileStmt //
    | expr SEMI #ExprStmt //
    | ID EQUALS expr SEMI #AssignStmt //
    | ID LRECT expr RRECT EQUALS expr SEMI #ArrayAssignStmt //
    ;

expr
    : LPAREN expr RPAREN #BinaryExpr //
    | 'new' INT LRECT expr RRECT #NewArrayExpr //
    | 'new' value=ID LPAREN RPAREN #NewObjectExpr //
    | LRECT (expr ( ',' expr)*)? RRECT #ArrayCreationExpr //
    | expr LRECT expr RRECT #ArrayAccessExpr //
    | expr '.' value=ID LPAREN (expr ( ',' expr )*)? RPAREN #MethodCallExpr //
    | expr '.' 'length' #ArrayLengthExpr //
    | value='this' #ThisExpr //
    | value= NOT expr #UnaryExpr //
    | expr op= (MUL | DIV) expr #BinaryExpr //
    | expr op= (ADD | SUB) expr #BinaryExpr
    | expr op= ('<' | '>') expr #BinaryExpr //
    | expr op= ('<='| '>=') expr #BinaryExpr //
    | expr op= ('==' | '!=') expr #BinaryExpr //
    | expr op= '&&' expr #BinaryExpr //
    | value=INTEGER #IntegerExpr //
    | value=BOOL #BooleanExpr //
    | value=ID #VarExpr //

    ;




grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

COMMENT: '//'.*?'\n' -> skip ;
BLOCK_COMMENT: '/*'.*?'*/' -> skip ;

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

INTEGER : '0' | [1-9][0-9]* ;
ID : [$a-zA-Z_][$a-zA-Z_0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDecl EOF;


importDeclaration
    : 'import' value+=ID( '.' value+=ID)* ('.*')? ';' ;


classDecl
    : CLASS name=ID ('extends' ext=ID)?
        LCURLY
        (varDecl)* (methodDecl)*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

functionType
    : type
    ;

returnStmt
    : 'return' expr
    ;

methodDecl locals[boolean isPublic=false, boolean isStatic = false]
    : (PUBLIC {$isPublic=true;})? type methodName=ID LPAREN (param)? RPAREN LCURLY (varDecl)* (stmt)* returnStmt SEMI RCURLY
    | (PUBLIC {$isPublic=true;})? (STATIC {$isStatic=true;}) type methodName=ID LPAREN STRING LRECT RRECT name=ID RPAREN LCURLY (varDecl)* (stmt)* RCURLY
    ;


type
    : type LRECT RRECT #ArrayType
    | type ELLIPSIS #ArrayType
    | value=INT     #IntType
    | value=BOOLEAN #BooleanType
    | value=ID      #ClassType
    | value=STRING  #StringType
    | value=VOID #VoidType
    ;


param
    : type paramName+=ID (COMMA type paramName+=ID)*
    ;

stmt
    : LCURLY (stmt)* RCURLY #BlockStmt //
    | 'if' LPAREN expr RPAREN stmt 'else' stmt #IfStmt //
    | 'while' LPAREN expr RPAREN stmt #WhileStmt //
    | expr SEMI #ExprStmt //
    | value=ID EQUALS expr SEMI #AssignStmt //
    | value+=ID LRECT expr RRECT EQUALS expr SEMI #ArrayAssignStmt //
    ;

expr
    : LPAREN expr RPAREN #ParenthesisExpr //
    | 'new' INT LRECT expr RRECT #NewArrayExpr //
    | 'new' value=ID LPAREN RPAREN #NewObjectExpr //
    | LRECT (expr ( ',' expr)*)? RRECT #ArrayCreationExpr //
    | expr LRECT expr RRECT #ArrayAccessExpr //
    | value=ID LPAREN (expr ( ',' expr )*)? RPAREN #MethodCallExpr //
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
    | value=INTEGER #IntegerLiteral //
    | value=BOOL #BooleanLiteral //
    | value=ID #VarRef //

    ;




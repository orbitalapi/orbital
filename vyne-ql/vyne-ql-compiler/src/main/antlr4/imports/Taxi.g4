grammar Taxi;

// starting point for parsing a taxi file
document
    :   (singleNamespaceDocument | multiNamespaceDocument)
    ;

singleNamespaceDocument
    :  importDeclaration* namespaceDeclaration? toplevelObject* EOF
    ;

multiNamespaceDocument
    : importDeclaration* namespaceBlock* EOF
    ;

importDeclaration
    :   'import' qualifiedName
    ;

namespaceDeclaration
    :   'namespace' qualifiedName
    ;

namespaceBlock
    :   'namespace' qualifiedName namespaceBody
    ;


namespaceBody
    : '{' toplevelObject* '}'
    ;

toplevelObject
    :   typeDeclaration
    |   enumDeclaration
    |   enumExtensionDeclaration
    |   typeExtensionDeclaration
    |   typeAliasDeclaration
    |   typeAliasExtensionDeclaration
    |   serviceDeclaration
    |   policyDeclaration
    |   functionDeclaration
    |   annotationTypeDeclaration
    ;

typeModifier
// A Parameter type indicates that the object
// is used when constructing requests,
// and that frameworks should freely construct
// these types based on known values.
    : 'parameter'
    | 'closed'
    ;

typeDeclaration
    :  typeDoc? annotation* typeModifier* ('type'|'model') Identifier

        ('inherits' listOfInheritedTypes)?
        typeBody?
    ;

listOfInheritedTypes
    : typeType (',' typeType)*
    ;
typeBody
    :   '{' (typeMemberDeclaration | conditionalTypeStructureDeclaration | calculatedMemberDeclaration)* '}'
    ;

typeMemberDeclaration
     :   typeDoc? annotation* fieldDeclaration
     ;

annotationTypeDeclaration
   : typeDoc? annotation* 'annotation' Identifier annotationTypeBody?;

annotationTypeBody: '{' typeMemberDeclaration* '}';

calculatedMemberDeclaration
   : typeMemberDeclaration  'as'
   (operatorExpression
   |
   calculatedExpression)
   ;


// THIS IS TO BE DEPRECRATED.  Use function infrastructrue, rather than adding new formulas
calculatedExpression:
           calculatedFormula '(' calculatedExpressionBody? ')'
           ;

// THIS IS TO BE DEPRECRATED.  Use function infrastructrue, rather than adding new formulas
calculatedFormula:
          'coalesce'
          ;

calculatedExpressionBody:
         typeType (',' typeType)*
         ;

operatorExpression
   : '(' typeType arithmaticOperator typeType ')'
   ;

fieldExpression
   : '(' propertyToParameterConstraintLhs arithmaticOperator propertyToParameterConstraintLhs ')'
   ;

conditionalTypeStructureDeclaration
    :
   '(' typeMemberDeclaration* ')' 'by' conditionalTypeConditionDeclaration
   ;

conditionalTypeConditionDeclaration:
   (fieldExpression |
   conditionalTypeWhenDeclaration);

conditionalTypeWhenDeclaration:
   'when' ('(' conditionalTypeWhenSelector ')')? '{'
   conditionalTypeWhenCaseDeclaration*
   '}';

conditionalTypeWhenSelector:
   mappedExpressionSelector |  // when( xpath('/foo/bar') : SomeType ) ...
   fieldReferenceSelector; // when( someField ) ...

mappedExpressionSelector: // xpath('/foo/bar') : SomeType
   scalarAccessorExpression ':' typeType;

// field references must be prefixed by this. -- ie., this.firstName
// this is to disambiguoate lookups by type -- ie., Name
//
// Note: Have had to relax the requirement for propertyFieldNameQualifier
// to be mandatory, as this created bacwards comapatbility issues
// in when() clauses
fieldReferenceSelector: propertyFieldNameQualifier? Identifier;
typeReferenceSelector: typeType;

conditionalTypeWhenCaseDeclaration:
   caseDeclarationMatchExpression '->' ( caseFieldAssignmentBlock |  caseScalarAssigningDeclaration);

caseFieldAssignmentBlock:
'{' caseFieldAssigningDeclaration*'}' ;


caseDeclarationMatchExpression: // when( ... ) {
   Identifier  | //  someField -> ...
   literal | //  'foo' -> ...
   enumSynonymSingleDeclaration | // some.Enum.EnumValue -> ...
   condition |
   caseElseMatchExpression;

caseElseMatchExpression: 'else';

caseFieldAssigningDeclaration :  // dealtAmount ...  (could be either a destructirng block, or an assignment)
   Identifier (
      caseFieldDestructuredAssignment | // dealtAmount ( ...
      ( '=' caseScalarAssigningDeclaration ) | // dealtAmount = ccy1Amount | dealtAmount = 'foo'
      // TODO : How do we model Enum assignments here?
      // .. some enum assignment ..
      scalarAccessor
   );

caseScalarAssigningDeclaration:
   caseFieldReferenceAssignment | literal | scalarAccessorExpression;

caseFieldDestructuredAssignment :  // dealtAmount ( ... )
     '(' caseFieldAssigningDeclaration* ')';

caseFieldReferenceAssignment : Identifier ('.' Identifier)*;

fieldModifier
   : 'closed'
   ;
fieldDeclaration
  :   fieldModifier? Identifier ':' typeType accessor?
  ;

typeType
    :   classOrInterfaceType typeArguments? listType? optionalType? parameterConstraint? aliasedType?
    ;

accessor : scalarAccessor | objectAccessor;

scalarAccessor
    : 'by' scalarAccessorExpression
    ;

scalarAccessorExpression
    : xpathAccessorDeclaration
    | jsonPathAccessorDeclaration
    | columnDefinition
    | conditionalTypeConditionDeclaration
    | defaultDefinition
    | readFunction
    ;

xpathAccessorDeclaration : 'xpath' '(' accessorExpression ')';
jsonPathAccessorDeclaration : 'jsonPath' '(' accessorExpression ')';

objectAccessor
    : '{' destructuredFieldDeclaration* '}'
    ;

destructuredFieldDeclaration
    : Identifier accessor
    ;
accessorExpression : StringLiteral;

classOrInterfaceType
    :   Identifier /* typeArguments? */ ('.' Identifier /* typeArguments? */ )*
    ;

typeArguments: '<' typeType (',' typeType)* '>';

// A "lenient" enum will match on case insensitive values
enumDeclaration
    :    typeDoc? annotation* lenientKeyword? 'enum' classOrInterfaceType
         (('inherits' enumInheritedType) | ('{' enumConstants? '}'))
    ;

enumInheritedType
    : typeType
    ;

enumConstants
    :   enumConstant (',' enumConstant)*
    ;

enumConstant
    :   typeDoc? annotation*  defaultKeyword? Identifier enumValue? enumSynonymDeclaration?
    ;

enumValue
   : '(' literal ')'
   ;

enumSynonymDeclaration
   : 'synonym' 'of' ( enumSynonymSingleDeclaration | enumSynonymDeclarationList)
   ;
enumSynonymSingleDeclaration : qualifiedName ;
enumSynonymDeclarationList : '[' qualifiedName (',' qualifiedName)* ']'
   ;
 enumExtensionDeclaration
    : typeDoc? annotation* 'enum extension' Identifier  ('{' enumConstantExtensions? '}')?
    ;

enumConstantExtensions
    :   enumConstantExtension (',' enumConstantExtension)*
    ;

enumConstantExtension
   : typeDoc? annotation* Identifier enumSynonymDeclaration?
   ;

// type aliases
typeAliasDeclaration
    : typeDoc? annotation* 'type alias' Identifier aliasedType
    ;

aliasedType
   : 'as' typeType
   ;

typeAliasExtensionDeclaration
   : typeDoc? annotation* 'type alias extension' Identifier
   ;
// Annotations
annotation
    :   '@' qualifiedName ( '(' ( elementValuePairs | elementValue )? ')' )?
    ;

elementValuePairs
    :   elementValuePair (',' elementValuePair)*
    ;

elementValuePair
    :   Identifier '=' elementValue
    ;

elementValue
    :   literal
    |    qualifiedName // Support enum references within annotations
    |   annotation
    ;

serviceDeclaration
    : typeDoc? annotation* 'service' Identifier serviceBody
    ;

serviceBody
    :   '{' serviceBodyMember* '}'
    ;
serviceBodyMember : serviceOperationDeclaration | queryOperationDeclaration;
// Querying
queryOperationDeclaration
   :  typeDoc? annotation* queryGrammarName 'query' Identifier '(' operationParameterList ')' ':' typeType
      'with' 'capabilities' '{' queryOperationCapabilities '}';

queryGrammarName : Identifier;
queryOperationCapabilities: (queryOperationCapability (',' queryOperationCapability)*);

queryOperationCapability:
   queryFilterCapability | Identifier;

queryFilterCapability: 'filter'( '(' filterCapability (',' filterCapability)* ')');

filterCapability: EQ | NQ | IN | LIKE | GT | GE | LT | LE;

serviceOperationDeclaration
     : typeDoc? annotation* operationScope? 'operation' operationSignature
     ;

operationSignature
     :   annotation* Identifier '(' operationParameterList? ')' operationReturnType?
     ;

operationScope : Identifier;

operationReturnType
    : ':' typeType
    ;
operationParameterList
    :   operationParameter (',' operationParameter)*
    ;

operationParameter
// Note that only one operationParameterConstraint can exist per parameter, but it can contain
// multiple expressions
     :   annotation* (parameterName)? typeType varargMarker?
     ;

varargMarker: '...';
// Parameter names are optional.
// But, they must be used to be referenced in return contracts
parameterName
    :   Identifier ':'
    ;

parameterConstraint
    :   '(' parameterConstraintExpressionList ')'
    |   '(' temporalFormatList ')'
    ;


parameterConstraintExpressionList
    :  parameterConstraintExpression (',' parameterConstraintExpression)*
    ;

parameterConstraintExpression
    :  propertyToParameterConstraintExpression
    |  operationReturnValueOriginExpression
    |  propertyFormatExpression
    ;

// First impl.  This will get richer (',' StringLiteral)*
propertyFormatExpression :
   '@format' '=' StringLiteral;

temporalFormatList :
   ('@format' '=' '[' StringLiteral (',' StringLiteral)* ']')? ','? (instantOffsetExpression)?
   ;

instantOffsetExpression :
   '@offset' '=' (IntegerLiteral | NegativeIntegerLiteral);

// The return value will have a relationship to a property
// received in an input (incl. nested properties)
operationReturnValueOriginExpression
    :  'from' qualifiedName
    ;

// A parameter will a value that matches a specified expression
// operation convertCurrency(request : ConversionRequest) : Money( this.currency = request.target )
// Models a constraint against an attribute on the type (generally return type).
// The attribute is identified by EITHER
// - it's name -- using this.fieldName
// - it's type (preferred) using TheTypeName
// The qualifiedName here is used to represent a path to the attribute (this.currency)
// We could've just used Identifier here, but we'd like to support nested paths
propertyToParameterConstraintExpression
   : propertyToParameterConstraintLhs comparisonOperator propertyToParameterConstraintRhs;

propertyToParameterConstraintLhs : propertyFieldNameQualifier? qualifiedName;
propertyToParameterConstraintRhs : (literal | qualifiedName);

propertyFieldNameQualifier : 'this' '.';

condition : logical_expr ;
logical_expr
 : logical_expr '&&' logical_expr # LogicalExpressionAnd
 | logical_expr '||' logical_expr  # LogicalExpressionOr
 | comparison_expr               # ComparisonExpression
 | logical_entity                # LogicalEntity
 ;

comparison_expr : comparison_operand comp_operator comparison_operand
                    # ComparisonExpressionWithOperator
                ;

comparison_operand : arithmetic_expr
                   ;

comp_operator : GT
              | GE
              | LT
              | LE
              | EQ
              | NQ
              ;

arithmetic_expr
 :  numeric_entity                        # ArithmeticExpressionNumericEntity
 ;

logical_entity : (TRUE | FALSE) # LogicalConst
               | propertyToParameterConstraintLhs     # LogicalVariable
               ;

numeric_entity : literal              # LiteralConst
               | propertyToParameterConstraintLhs           # NumericVariable
               ;

comparisonOperator
   : '='
   | '>'
   | '>='
   | '<='
   | '<'
   ;

arithmaticOperator
   : '+'
   | '-'
   | '*'
   | '/'
   ;



policyDeclaration
    :  annotation* 'policy' policyIdentifier 'against' typeType '{' policyRuleSet* '}';

policyOperationType
    : Identifier;

policyRuleSet : policyOperationType policyScope? '{' (policyBody | policyInstruction) '}';

policyScope : 'internal' | 'external';


policyBody
    :   policyStatement*
    ;

policyIdentifier : Identifier;

policyStatement
    : policyCase | policyElse;

// TODO: Should consider revisiting this, so that operators are followed by valid tokens.
// eg: 'in' must be followed by an array.  We could enforce this at the language, to simplify in Vyne
policyCase
    : 'case' policyExpression policyOperator policyExpression '->' policyInstruction
    ;

policyElse
    : 'else' '->' policyInstruction
    ;
policyExpression
    : callerIdentifer
    | thisIdentifier
    | literalArray
    | literal;


callerIdentifer : 'caller' '.' typeType;
thisIdentifier : 'this' '.' typeType;

// TODO: Should consider revisiting this, so that operators are followed by valid tokens.
// eg: 'in' must be followed by an array.  We could enforce this at the language, to simplify in Vyne
policyOperator
    : EQ
    | NQ
    | IN
    ;

literalArray
    : '[' literal (',' literal)* ']'
    ;

policyInstruction
    : policyInstructionEnum
    | policyFilterDeclaration
    ;

policyInstructionEnum
    : 'permit';

policyFilterDeclaration
    : 'filter' filterAttributeNameList?
    ;

filterAttributeNameList
    : '(' Identifier (',' Identifier)* ')'
    ;

// processors currently disabled
// https://gitlab.com/vyne/vyne/issues/52
//policyProcessorDeclaration
//    : 'process' 'using' qualifiedName policyProcessorParameterList?
//    ;

//policyProcessorParameterList
//    : '(' policyParameter (',' policyParameter)* ')'
//    ;

//policyParameter
//    : literal | literalArray;
//

columnDefinition : 'column' '(' columnIndex ')' ;

defaultDefinition: 'default' '(' literal ')';

// "declare function" borrowed from typescript.
// Note that taxi supports declaring a function, but won't provide
// an implementation of it.  That'll be down to individual libraries
// Note - intentional decision to enforce these functions to return something,
// rather than permitting void return types.
// This is because in a mapping declaration, functions really only have purpose if
// they return things.
functionDeclaration: 'declare' 'function' functionName '(' operationParameterList? ')' ':' typeType;

// Deprecated, use functionDeclaration
readFunction: functionName '(' formalParameterList? ')';
//         'concat' |
//         'leftAndUpperCase' |
//         'midAndUpperCase'
//         ;
functionName: qualifiedName;
formalParameterList
    : parameter  (',' parameter)*
    ;
//    scalarAccessorExpression
      //    : xpathAccessorDeclaration
      //    | jsonPathAccessorDeclaration
      //    | columnDefinition
      //    | conditionalTypeConditionDeclaration
      //    | defaultDefinition
      //    | readFunction
      //    ;
parameter: literal |  scalarAccessorExpression | fieldReferenceSelector | typeReferenceSelector;

columnIndex : IntegerLiteral | StringLiteral;

expression
    :   primary
    ;

primary
    :   '(' expression ')'
//    |   'this'
//    |   'super'
    |   literal
    |   Identifier
//    |   typeType '.' 'class'
//    |   'void' '.' 'class'
//    |   nonWildcardTypeArguments (explicitGenericInvocationSuffix | 'this' arguments)
    ;

qualifiedName
    :   Identifier ('.' Identifier)*
    ;

listType
   : '[]'
   ;

optionalType
   : '?'
   ;

//primitiveType
//    : primitiveTypeName
//    | 'lang.taxi.' primitiveTypeName
//    ;
//
//primitiveTypeName
//    :   'Boolean'
//    |   'String'
//    |   'Int'
//    |   'Double'
//    |   'Decimal'
////    The "full-date" notation of RFC3339, namely yyyy-mm-dd. Does not support time or time zone-offset notation.
//    |   'Date'
////    The "partial-time" notation of RFC3339, namely hh:mm:ss[.ff...]. Does not support date or time zone-offset notation.
//    |   'Time'
//// Combined date-only and time-only with a separator of "T", namely yyyy-mm-ddThh:mm:ss[.ff...]. Does not support a time zone offset.
//    |   'DateTime'
//// A timestamp, indicating an absolute point in time.  Includes timestamp.  Should be rfc3339 format.  (eg: 2016-02-28T16:41:41.090Z)
//    |   'Instant'
//    |  'Any'
//    ;

// https://github.com/raml-org/raml-spec/blob/master/versions/raml-10/raml-10.md#date
literal
    :   IntegerLiteral
    |   BooleanLiteral
    |   StringLiteral
    |   'null'
    ;

typeExtensionDeclaration
   :  typeDoc? annotation* 'type extension' Identifier typeExtensionBody
   ;

typeExtensionBody
    :   '{' typeExtensionMemberDeclaration* '}'
    ;

typeExtensionMemberDeclaration
    :   annotation* typeExtensionFieldDeclaration
    ;

typeExtensionFieldDeclaration
    :   Identifier typeExtensionFieldTypeRefinement?
    ;

typeExtensionFieldTypeRefinement
    : ':' typeType constantDeclaration?
    ;

constantDeclaration : 'with' 'default' (literal | qualifiedName);
// Typedoc is a special documentation block that wraps types.
// It's treated as plain text, but we'll eventually support doc tools
// that speak markdown.
// Comment markers are [[ .... ]], as this is less likely to generate clashes.
typeDoc
 : '[[' ( ~']]' | '"' | '\'')* ']]';


lenientKeyword: 'lenient';
defaultKeyword: 'default';

IN: 'in';
LIKE: 'like';
AND : 'and' ;
OR  : 'or' ;

Identifier
    :   Letter LetterOrDigit*
    | '`' ~('`')+ '`'
    ;


StringLiteral
    :   '"' (DoubleQuoteStringCharacter+)? '"'
    |   '\'' (SingleQuoteStringCharacter+)? '\''
    ;


BooleanLiteral
    :   'true' | 'false'
    ;


fragment
DoubleQuoteStringCharacter
    :   ~["\\]
    |   EscapeSequence
    ;

fragment
SingleQuoteStringCharacter
    :   ~["'\\]
    |   EscapeSequence
    ;

// §3.10.6 Escape Sequences for Character and String Literals

fragment
EscapeSequence
    :   '\\' [btnfr"'\\]
//    |   OctalEscape
//    |   UnicodeEscape
    ;


fragment
Letter
    :   [a-zA-Z$_] // these are the "java letters" below 0x7F
    |   // covers all characters above 0x7F which are not a surrogate
        ~[\u0000-\u007F\uD800-\uDBFF]
    |   // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        [\uD800-\uDBFF] [\uDC00-\uDFFF]
    ;

fragment
LetterOrDigit
    :   [a-zA-Z0-9$_] // these are the "java letters or digits" below 0x7F
    |   // covers all characters above 0x7F which are not a surrogate
        ~[\u0000-\u007F\uD800-\uDBFF]
    |   // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        [\uD800-\uDBFF] [\uDC00-\uDFFF]
    ;
NegativeIntegerLiteral
   : '-' IntegerLiteral
   ;

IntegerLiteral
    :   DecimalNumeral /* IntegerTypeSuffix? */
    ;

fragment
DecimalNumeral
    :   '0'
    |   NonZeroDigit (Digits? | Underscores Digits)
    ;

fragment
Digits
    :   Digit (DigitOrUnderscore* Digit)?
    ;

fragment
Digit
    :   '0'
    |   NonZeroDigit
    ;

fragment
NonZeroDigit
    :   [1-9]
    ;

fragment
DigitOrUnderscore
    :   Digit
    |   '_'
    ;

fragment
Underscores
    :   '_'+
    ;

NAME
   : [_A-Za-z] [_0-9A-Za-z]*
   ;


STRING
   : '"' ( ESC | ~ ["\\] )* '"'
   ;


fragment ESC
   : '\\' ( ["\\/bfnrt] | UNICODE )
   ;


fragment UNICODE
   : 'u' HEX HEX HEX HEX
   ;


fragment HEX
   : [0-9a-fA-F]
   ;


NUMBER
   : '-'? INT '.' [0-9]+ EXP? | '-'? INT EXP | '-'? INT
   ;


fragment INT
   : '0' | [1-9] [0-9]*
   ;

fragment EXP
   : [Ee] [+\-]? INT
   ;

//
// Whitespace and comments
//

WS  :  [ \t\r\n\u000C]+ -> skip
    ;

COMMENT
    :   '/*' .*? '*/' -> channel(HIDDEN)
    ;

LINE_COMMENT
    :   '//' ~[\r\n]* -> channel(HIDDEN)
    ;


GT : '>' ;
GE : '>=' ;
LT : '<' ;
LE : '<=' ;
EQ : '=' ;
NQ : '!=';

TRUE  : 'true' ;
FALSE : 'false' ;

MULT  : '*' ;
DIV   : '/' ;
PLUS  : '+' ;
MINUS : '-' ;

LPAREN : '(' ;
RPAREN : ')' ;

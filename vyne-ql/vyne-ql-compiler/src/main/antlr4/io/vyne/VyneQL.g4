grammar VyneQL;
import Taxi;

queryDocument: importDeclaration* query EOF;

query: namedQuery | anonymousQuery;

namedQuery: queryName '{' queryBody '}';
anonymousQuery: queryBody;

queryName: 'query' Identifier queryParameters?;

queryParameters: '(' queryParamList ')';

queryParamList: queryParam (',' queryParam)*;

queryParam: Identifier ':' typeType;

findAllDirective: 'findAll';
findOneDirective: 'findOne';

queryDirective: findAllDirective | findOneDirective;

givenBlock : 'given' '{' factList '}';

factList : fact (',' fact)*;

// TODO :  We could/should make variableName optional
fact : variableName typeType '=' literal;

variableName: Identifier ':';
queryBody:
   givenBlock?
	queryDirective '{' queryTypeList '}' queryProjection?;

queryTypeList: typeType (',' typeType)*;

queryProjection: 'as' typeType? anonymousTypeDefinition?;
//as {
//    orderId // if orderId is defined on the Order type, then the type is inferrable
//    productId: ProductId // Discovered, using something in the query context, it's up to Vyne to decide how.
//    traderEmail : EmailAddress(from this.traderUtCode)
//    salesPerson {
//        firstName : FirstName
//        lastName : LastName
//    }(from this.salesUtCode)
//}
anonymousTypeDefinition: '{' anonymousField*  '}' listType?;
anonymousFieldDeclaration: Identifier ':' typeType;
anonymousFieldDeclarationWithSelfReference: Identifier ':' typeType '(' 'from' 'this.' Identifier ')';
anonymousComplexFieldDeclaration: Identifier '{' (Identifier ':' typeType)*  '}'  '(' 'from' 'this.' Identifier ')';

anonymousField:
   Identifier  // e.g. orderId
   |
   anonymousFieldDeclaration // productId: ProductId
   |
   anonymousFieldDeclarationWithSelfReference // traderEmail : EmailAddress(from this.traderUtCode)
   |
   anonymousComplexFieldDeclaration //    salesPerson {
                                    //        firstName : FirstName
                                    //        lastName : LastName
                                    //    }(from this.salesUtCode)
   ;


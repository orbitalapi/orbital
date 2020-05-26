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

queryProjection: 'as' typeType; // TODO : Inline type spec

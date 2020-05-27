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

queryBody:
	queryDirective '{' queryTypeList '}' queryProjection?;

queryTypeList: typeType (',' typeType)*;

queryProjection: 'as' typeType; // TODO : Inline type spec

import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs/internal/Observable';

import {environment} from 'src/environments/environment';
import {
  DataSource,
  InstanceLikeOrCollection, Proxyable,
  QualifiedName,
  ReferenceOrInstance,
  Type,
  TypedInstance,
  TypeNamedInstance
} from './schema';
import {VyneServicesModule} from './vyne-services.module';

@Injectable({
  providedIn: VyneServicesModule
})

export class QueryService {

  httpOptions = {
    headers: new HttpHeaders({
      'Content-Type':  'application/json',
      'Accept': 'application/json'
    })
  };

  constructor(private http: HttpClient) {

  }

  submitQuery(query: Query): Observable<QueryResult> {
    return this.http.post<QueryResult>(`${environment.queryServiceUrl}/api/query`, query, this.httpOptions);
  }

  submitVyneQlQuery(query: String, resultMode: ResultMode = ResultMode.VERBOSE): Observable<QueryResult> {
    return this.http.post<QueryResult>(`${environment.queryServiceUrl}/api/vyneql?resultMode=${resultMode}`, query, this.httpOptions);
  }

  submitVyneQlQueryStreaming(query: String, resultMode: ResultMode = ResultMode.VERBOSE): Observable<QueryResult> {
    return Observable.create( observer => {

    })

    return this.http.post<QueryResult>(`${environment.queryServiceUrl}/api/vyneql?resultMode=${resultMode}`, query, this.httpOptions);
  }

  getHistoryRecord(queryId: string): Observable<QueryHistoryRecord> {
    return this.http.get<QueryHistoryRecord>(`${environment.queryServiceUrl}/api/query/history/${queryId}`, this.httpOptions);
  }

  getHistory(): Observable<QueryHistorySummary[]> {
    return this.http.get<QueryHistorySummary[]>(`${environment.queryServiceUrl}/api/query/history`, this.httpOptions);
  }

  getQueryResultNodeDetail(queryId: string, requestedTypeInQuery: QualifiedName, nodeId: string): Observable<QueryResultNodeDetail> {
    const safeNodeId = encodeURI(nodeId);
    return this.http.get<QueryResultNodeDetail>(
      `${environment.queryServiceUrl}/api/query/history/${queryId}/${requestedTypeInQuery.parameterizedName}/${safeNodeId}`, this.httpOptions
    );
  }

  getQueryProfile(queryId: string): Observable<ProfilerOperation> {
    return this.http.get<ProfilerOperation>(`${environment.queryServiceUrl}/api/query/history/${queryId}/profile`, this.httpOptions);
  }

  invokeOperation(serviceName: string, operationName: string, parameters: { [index: string]: Fact }): Observable<TypedInstance> {
    return this.http.post<TypedInstance>(`${environment.queryServiceUrl}/api/services/${serviceName}/${operationName}`, parameters, this.httpOptions);
  }
}

export class Query {
  constructor(readonly expression: TypeNameListQueryExpression,
              readonly facts: Fact[],
              readonly queryMode: QueryMode,
              readonly resultMode: ResultMode) {
  }
}

export interface TypeNameListQueryExpression {
  typeNames: string[];

  // Received from the server, but don't need to send it up
  qualifiedTypeNames?: QualifiedName[] | null;
}

export class Fact {
  constructor(readonly typeName: string, readonly  value: any) {
  }

  qualifiedName: QualifiedName | null; // sent from the server, not required when sending to the server
}


export interface QueryResultNodeDetail {
  attributeName: string;
  path: string;
  typeName: QualifiedName;
  source: DataSource;
}


export function isOperationResult(source: DataSource): source is OperationResultDataSource {
  return source.dataSourceName === 'Operation result';
}

export interface OperationResultDataSource extends DataSource {
  remoteCall: ReferenceOrInstance<RemoteCall>;
  inputs: OperationParam[];
}

export function isEvaluatedExpressionDataSource(source: DataSource): source is EvaluatedExpressionDataSource {
  return source.dataSourceName === 'Evaluated expression';
}

export interface EvaluatedExpressionDataSource extends DataSource {
  expressionTaxi: string;
  inputs: TypeNamedInstance[];
}

export function isFailedEvaluatedExpressionDataSource(source: DataSource): source is FailedEvaluatedExpressionDataSource {
  return source.dataSourceName === 'Failed evaluated expression';
}

export interface FailedEvaluatedExpressionDataSource extends EvaluatedExpressionDataSource {
  errorMessage: string;
}

export interface OperationParam {
  parameterName: String;
  value: TypeNamedInstance;
}


export enum ResponseStatus {
  COMPLETED = 'COMPLETED',
  // Ie., the query didn't error, but not everything was resolved
  INCOMPLETE = 'INCOMPLETE',
  ERROR = 'ERROR',
}

export interface QueryResult {
  results: { [key: string]: InstanceLikeOrCollection };
  unmatchedNodes: QualifiedName[];
  fullyResolved: boolean;
  profilerOperation: ProfilerOperation;
  remoteCalls: RemoteCall[];
  resultMode: ResultMode;
  queryResponseId: string;
  error?: string;
  responseStatus: ResponseStatus;
  anonymousTypes: Type[];
}


export interface RemoteCall extends Proxyable {
  service: string;
  address: string;
  operation: string;
  responseTypeName: string;
  method: string;
  requestBody: any;
  resultCode: number;
  durationMs: number;
  response: any;
  operationQualifiedName: string;
}

export interface ProfilerOperation {
  id: string;
  fullPath: string;
  path: string;
  componentName: string;
  operationName: string;
  children: ProfilerOperation[];
  result: ProfilerOperationResult;

  duration: number;

  context: any;

  description: string;
}

export interface ProfilerOperationResult {
  startTime: number;
  endTime: number;
  value: any;

  duration: number;

}

export enum QueryMode {
  DISCOVER = 'DISCOVER',
  BUILD = 'BUILD',
  GATHER = 'GATHER'
}

export enum ResultMode {
  SIMPLE = 'SIMPLE',
  VERBOSE = 'VERBOSE'
}

export interface VyneQlQueryHistoryRecord extends QueryHistoryRecord {
  query: string;
}

export interface RestfulQueryHistoryRecord extends QueryHistoryRecord {
  query: Query;
}

export interface QueryHistoryRecord {
  response: QueryResult;
  timestamp: Date;
  id: string;
}

export interface RestfulQueryHistorySummary extends QueryHistorySummary {
  query: Query;
}

export interface VyneQlQueryHistorySummary extends QueryHistorySummary {
  query: string;
}

export interface QueryHistorySummary {
  queryId: string;
  responseStatus: ResponseStatus;
  durationMs: number;
  recordSize: number;
  timestamp: Date;
}

export function isVyneQlQueryHistorySummaryRecord(value: QueryHistorySummary): value is VyneQlQueryHistorySummary {
  return typeof value['query'] === 'string';
}

export function isVyneQlQueryHistoryRecord(value: QueryHistoryRecord): value is VyneQlQueryHistoryRecord {
  return typeof value['query'] === 'string';
}

export function isRestQueryHistoryRecord(value: QueryHistoryRecord): value is RestfulQueryHistoryRecord {
  return (value as RestfulQueryHistoryRecord).query.queryMode !== undefined;
}

export function isRestQueryHistorySummaryRecord(value: QueryHistorySummary): value is RestfulQueryHistorySummary {
  return (value as RestfulQueryHistorySummary).query.queryMode !== undefined;
}


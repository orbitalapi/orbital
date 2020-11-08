import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs/internal/Observable';

import {environment} from 'src/environments/environment';
import {QualifiedName, TypedInstance} from './schema';
import {InstanceLikeOrCollection} from '../object-view/object-view.component';
import {VyneServicesModule} from './vyne-services.module';
import {Data} from '@angular/router';
import {isNullOrUndefined} from 'util';

@Injectable({
  providedIn: VyneServicesModule
})
export class QueryService {
  constructor(private http: HttpClient) {
  }

  submitQuery(query: Query): Observable<QueryResult> {
    return this.http.post<QueryResult>(`${environment.queryServiceUrl}/api/query`, query);
  }

  submitVyneQlQuery(query: String, resultMode: ResultMode = ResultMode.VERBOSE): Observable<QueryResult> {
    const headers = new HttpHeaders().set('Content-Type', 'application/json');
    return this.http.post<QueryResult>(`${environment.queryServiceUrl}/api/vyneql?resultMode=${resultMode}`, query, {headers});
  }

  getHistoryRecord(queryId: string): Observable<QueryHistoryRecord> {
    return this.http.get<QueryHistoryRecord>(`${environment.queryServiceUrl}/api/query/history/${queryId}`);
  }

  getHistory(): Observable<QueryHistorySummary[]> {
    return this.http.get<QueryHistorySummary[]>(`${environment.queryServiceUrl}/api/query/history`);
  }

  getQueryResultNodeDetail(queryId: string, requestedTypeInQuery: QualifiedName, nodeId: string): Observable<QueryResultNodeDetail> {
    const safeNodeId = encodeURI(nodeId);
    return this.http.get<QueryResultNodeDetail>(
      `${environment.queryServiceUrl}/api/query/history/${queryId}/${requestedTypeInQuery.parameterizedName}/${safeNodeId}`
    );
  }

  getQueryProfile(queryId: string): Observable<ProfilerOperation> {
    return this.http.get<ProfilerOperation>(`${environment.queryServiceUrl}/api/query/history/${queryId}/profile`);
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

export interface TypeNamedInstance {
  typeName: string;
  value: any;
  source?: DataSourceReference;
}

export interface QueryResultNodeDetail {
  attributeName: string;
  path: string;
  typeName: QualifiedName;
  source: DataSource;
}

export function isTypedInstance(instance: InstanceLikeOrCollection): instance is TypedInstance {
  const instanceAny = instance as any;
  return instanceAny && instanceAny.type !== undefined && instanceAny.value !== undefined;
}

export function isTypedNull(instance: InstanceLikeOrCollection): instance is TypedInstance {
  const instanceAny = instance as any;
  return instanceAny && instanceAny.type !== undefined && isNullOrUndefined(instanceAny.value);
}

export function isTypeNamedInstance(instance: any): instance is TypeNamedInstance {
  const instanceAny = instance as any;
  return instanceAny && instanceAny.typeName !== undefined && instanceAny.value !== undefined;
}

export function isTypeNamedNull(instance: any): instance is TypeNamedInstance {
  const instanceAny = instance as any;
  return instanceAny && instanceAny.typeName !== undefined && isNullOrUndefined(instanceAny.value);
}

export function isTypedCollection(instance: any): instance is TypeNamedInstance[] {
  return instance && Array.isArray(instance) && instance[0] && isTypeNamedInstance(instance[0]);
}

export interface DataSourceReference {
  dataSourceIndex: number;
}

export interface LineageGraph {
  [index: number]: DataSource;
}

export interface DataSource {
  name: DataSourceType;
}

export type DataSourceType =
  'Provided'
  | 'Mapped'
  | 'Operation result'
  | 'Defined in schema'
  | 'Undefined source'
  | 'Multiple sources';

export function isOperationResult(source: DataSource): source is OperationResultDataSource {
  return source.name === 'Operation result';
}

export interface OperationResultDataSource extends DataSource {
  remoteCall: RemoteCall;
  inputs: OperationParam[];
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
  lineageGraph: LineageGraph;
  queryResponseId: string;
  error?: string;
  responseStatus: ResponseStatus;
}


export interface RemoteCall {
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


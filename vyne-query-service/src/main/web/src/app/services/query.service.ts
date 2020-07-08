import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs/internal/Observable';

import {environment} from 'src/environments/environment';
import {QualifiedName, TypedInstance} from './schema';
import {InstanceLikeOrCollection} from '../object-view/object-view.component';
import {VyneServicesModule} from './vyne-services.module';
import {Data} from '@angular/router';

@Injectable({
  providedIn: VyneServicesModule
})
export class QueryService {
  constructor(private http: HttpClient) {
  }

  submitQuery(query: Query): Observable<QueryResult> {
    return this.http.post<QueryResult>(`${environment.queryServiceUrl}/api/query`, query);
  }

  submitVyneQlQuery(query: String): Observable<QueryResult> {
    return this.http.post<QueryResult>(`${environment.queryServiceUrl}/api/vyneql`, query);
  }

  getHistory(): Observable<QueryHistoryRecord[]> {
    return this.http.get<QueryHistoryRecord[]>(`${environment.queryServiceUrl}/api/query/history`);
  }

  getQueryProfile(queryId: string): Observable<ProfilerOperation> {
    return this.http.get<ProfilerOperation>(`${environment.queryServiceUrl}/api/query/history/${queryId}/profile`);
  }
}

export class Query {
  constructor(readonly expression: string[],
              readonly facts: Fact[],
              readonly queryMode: QueryMode,
              readonly resultMode: ResultMode) {
  }
}

export class Fact {
  constructor(readonly typeName: string, readonly  value: any) {
  }
}

export interface TypeNamedInstance {
  typeName: string;
  value: any;
  source?: DataSourceReference;
}

export function isTypedInstance(instance: InstanceLikeOrCollection): instance is TypedInstance {
  const instanceAny = instance as any;
  return instanceAny && instanceAny.type !== undefined && instanceAny.value !== undefined;
}

export function isTypeNamedInstance(instance: any): instance is TypeNamedInstance {
  const instanceAny = instance as any;
  return instanceAny && instanceAny.typeName !== undefined && instanceAny.value !== undefined;
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


export interface QueryResult {
  results: { [key: string]: InstanceLikeOrCollection };
  unmatchedNodes: QualifiedName[];
  fullyResolved: boolean;
  profilerOperation: ProfilerOperation;
  remoteCalls: RemoteCall[];
  resultMode: ResultMode;
  lineageGraph: LineageGraph;
  queryResponseId: string;
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

export function isVyneQlQueryHistoryRecord(value: QueryHistoryRecord): value is VyneQlQueryHistoryRecord {
  return typeof value['query'] === 'string';
}

export function isRestQueryHistoryRecord(value: QueryHistoryRecord): value is RestfulQueryHistoryRecord {
  return (value as RestfulQueryHistoryRecord).query.queryMode !== undefined;
}


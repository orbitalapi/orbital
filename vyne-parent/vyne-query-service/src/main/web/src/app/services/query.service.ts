import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs/internal/Observable";

import {environment} from 'src/environments/environment';
import {QualifiedName} from "./schema";

@Injectable()
export class QueryService {
  constructor(private http: HttpClient) {
  }

  submitQuery(query: Query): Observable<QueryResult> {
    return this.http.post<QueryResult>(`${environment.queryServiceUrl}/query`, query)
  }

  getHistory(): Observable<QueryHistoryRecord[]> {
    return this.http.get<QueryHistoryRecord[]>(`${environment.queryServiceUrl}/query/history`)
  }

  getQueryProfile(queryId: string): Observable<ProfilerOperation> {
    return this.http.get<ProfilerOperation>(`${environment.queryServiceUrl}/query/history/${queryId}/profile`)
  }
}

export class Query {
  constructor(readonly expression: string[],
              readonly facts: Fact[],
              readonly queryMode: QueryMode) {
  }
}

export class Fact {
  constructor(readonly typeName: string, readonly  value: any) {
  }
}

export interface TypeNamedInstance {
  typeName: string;
  value: any;
}


export type TypedInstanceOrCollection = TypeNamedInstance | TypeNamedInstance[]; // TODO : Could also be a map.
export interface QueryResult {
  results: { [key: string]: TypedInstanceOrCollection };

  unmatchedNodes: QualifiedName[];
  fullyResolved: boolean;
  profilerOperation: ProfilerOperation;
  remoteCalls: RemoteCall[]
}

export interface RemoteCall {
  service: QualifiedName;
  operation: string;
  method: string;
  requestBody: any;
  resultCode: number;
  durationMs: number;
  response: any;
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

  description: string
}

export interface ProfilerOperationResult {
  startTime: number;
  endTime: number;
  value: any;

  duration: number;

}

export enum QueryMode {
  DISCOVER = "DISCOVER",
  GATHER = "GATHER"
}


export interface QueryHistoryRecord {
  query: Query;
  response: QueryResult;
  timestamp: Date;
  id: string
}

import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs/internal/Observable";

import {environment} from 'src/environments/environment';

@Injectable()
export class QueryService {
  constructor(private http: HttpClient) {
  }

  submitQuery(query: Query): Observable<QueryResult> {
    return this.http.post<QueryResult>(`${environment.queryServiceUrl}/query`, query)
  }

}

export class Query {
  constructor(readonly queryString: string,
              readonly facts: any) {
  }
}

export interface QueryResult {
  results: any;
  unmatchedNodes: string[];
  isFullyResolved: boolean;
  profilerOperation: ProfilerOperation;
}

export interface ProfilerOperation {
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

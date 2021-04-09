/* tslint:disable:max-line-length */
import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs/internal/Observable';
import {nanoid} from 'nanoid';
import {environment} from 'src/environments/environment';
import {
  DataSource, InstanceLike,
  InstanceLikeOrCollection, Proxyable,
  QualifiedName,
  ReferenceOrInstance,
  Type,
  TypedInstance, TypedObjectAttributes,
  TypeNamedInstance
} from './schema';
import {VyneServicesModule} from './vyne-services.module';
import {concatAll, map, share, shareReplay} from 'rxjs/operators';
import {SseEventSourceService} from './sse-event-source.service';
import {isNullOrUndefined} from 'util';

@Injectable({
  providedIn: VyneServicesModule
})

export class QueryService {

  httpOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    })
  };

  constructor(private http: HttpClient,
              private sse: SseEventSourceService) {

  }

  submitQuery(query: Query, clientQueryId: string, resultMode: ResultMode = ResultMode.SIMPLE): Observable<ValueWithTypeName> {
    // TODO :  I suspect the return type here is actually ValueWithTypeName | ValueWithTypeName[]
    return this.http.post<ValueWithTypeName[]>(`${environment.queryServiceUrl}/api/query?resultMode=${resultMode}&clientQueryId=${clientQueryId}`, query, this.httpOptions)
      // the legaacy (blocking) endpoint returns a ValueWithTypeName[].
      // however, we want to unpack that to multiple emitted items on our observable
      // therefore, concatAll() seems to do this.
      // https://stackoverflow.com/questions/42482705/best-way-to-flatten-an-array-inside-an-rxjs-observable
      .pipe(concatAll());

  }

  submitVyneQlQueryStreaming(query: string, clientQueryId: string, resultMode: ResultMode = ResultMode.SIMPLE, replayCacheSize = 500): Observable<ValueWithTypeName> {
    const url = encodeURI(`${environment.queryServiceUrl}/api/vyneql?resultMode=${resultMode}&clientQueryId=${clientQueryId}&query=${query}`);
    return this.sse.getEventSource(
      url
    ).pipe(
      map((event: MessageEvent) => {
        return JSON.parse(event.data) as ValueWithTypeName;
      }),
      shareReplay(replayCacheSize)
    );
  }

  getQueryResults(queryId: string, limit: number = 100): Observable<ValueWithTypeName> {
    const url = encodeURI(`${environment.queryServiceUrl}/api/query/history/${queryId}/results?limit=${limit}`);
    return this.sse.getEventSource(
      url
    ).pipe(
      map((event: MessageEvent) => {
        return JSON.parse(event.data) as ValueWithTypeName;
      }),
      shareReplay(limit)
    );
  }

  getHistory(): Observable<QueryHistorySummary[]> {
    return this.http.get<QueryHistorySummary[]>(`${environment.queryServiceUrl}/api/query/history`, this.httpOptions);
  }

  getQueryResultNodeDetail(queryId: string, rowValueId: number, attributePath: string): Observable<QueryResultNodeDetail> {
    return this.http.get<QueryResultNodeDetail>(
      `${environment.queryServiceUrl}/api/query/history/${queryId}/dataSource/${rowValueId}/${attributePath}`, this.httpOptions
    );
  }

  getQueryResultNodeDetailFromClientId(clientQueryId: string, rowValueId: number, attributePath: string): Observable<QueryResultNodeDetail> {
    return this.http.get<QueryResultNodeDetail>(
      `${environment.queryServiceUrl}/api/query/history/clientId/${clientQueryId}/dataSource/${rowValueId}/${attributePath}`, this.httpOptions
    );
  }


  getQueryProfileFromClientId(clientQueryId: string): Observable<QueryProfileData> {
    return this.http.get<QueryProfileData>(`${environment.queryServiceUrl}/api/query/history/clientId/${clientQueryId}/profile`, this.httpOptions)
      .pipe(shareReplay(1)) // This observable is shared
      ;
  }

  getQueryProfile(queryId: string): Observable<QueryProfileData> {
    return this.http.get<QueryProfileData>(`${environment.queryServiceUrl}/api/query/history/${queryId}/profile`, this.httpOptions)
      .pipe(shareReplay(1)) // This observable is shared
      ;
  }

  invokeOperation(serviceName: string, operationName: string, parameters: { [index: string]: Fact }): Observable<TypedInstance> {
    return this.http.post<TypedInstance>(`${environment.queryServiceUrl}/api/services/${serviceName}/${operationName}`, parameters, this.httpOptions);
  }

  cancelQuery(queryId: string): Observable<void> {
    return null;
  }
}

export interface QueryMetadata {
  remoteCalls: RemoteCall[];
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
  profilerOperation: QueryProfileData;
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

export interface QueryProfileData {
  id: string;
  duration: number;
  remoteCalls: RemoteCall[];
  timings: any; // TODO
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

export interface QueryHistorySummary {
  queryId: string;
  clientQueryId: string;
  taxiQl: string | null;
  queryJson: Query | null;
  startTime: Date;
  endTime: Date | null;
  responseStatus: ResponseStatus;
  durationMs: number;
  recordCount: number;
  errorMessage: string | null;
}

/**
 * During a streaming query, we can receive any of these message types
 */
export type StreamingQueryMessage = ValueWithTypeName | FailedSearchResponse;

export function isFailedSearchResponse(message: StreamingQueryMessage): message is FailedSearchResponse {
  return !isNullOrUndefined(message['responseStatus']) && message['responseStatus'] === ResponseStatus.ERROR;
}

export function isValueWithTypeName(message: StreamingQueryMessage): message is ValueWithTypeName {
  return !isNullOrUndefined(message['value']);
}

export interface ValueWithTypeName {
  typeName: string | null;
  anonymousTypes: Type[];
  /**
   * This is the serialized instance, as converted by a RawObjectMapper.
   * It's a raw json object.
   * Use TypedObjectAttributes here, rather than any, as it's compatible with InstanceLike interface
   */
  value: TypedObjectAttributes;
  valueId: number;
}

export interface FailedSearchResponse {
  message: string;
  responseStatus: ResponseStatus;
  queryResponseId: string | null;
  clientQueryId: string | null;
  remoteCalls: RemoteCall[];
}

export function randomId(): string {
  return nanoid();
}

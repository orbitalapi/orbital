/* eslint-disable max-len */
import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs/internal/Observable';
import {nanoid} from 'nanoid';
import {environment} from 'src/environments/environment';
import {
  DataSource,
  InstanceLikeOrCollection,
  Proxyable,
  QualifiedName,
  ReferenceOrInstance,
  Type,
  TypedInstance,
  TypeNamedInstance
} from './schema';
import {VyneServicesModule} from './vyne-services.module';
import {catchError, concatAll, map, shareReplay} from 'rxjs/operators';
import {SseEventSourceService} from './sse-event-source.service';
import {of} from 'rxjs';
import {FailedSearchResponse, StreamingQueryMessage, ValueWithTypeName} from './models';

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

  submitQuery(query: Query, clientQueryId: string, resultMode: ResultMode = ResultMode.SIMPLE, replayCacheSize = 500): Observable<ValueWithTypeName> {
    // TODO :  I suspect the return type here is actually ValueWithTypeName | ValueWithTypeName[]
    return this.http.post<ValueWithTypeName[]>(`${environment.queryServiceUrl}/api/query?resultMode=${resultMode}&clientQueryId=${clientQueryId}`, query, this.httpOptions)
      .pipe(
        // the legaacy (blocking) endpoint returns a ValueWithTypeName[].
        // however, we want to unpack that to multiple emitted items on our observable
        // therefore, concatAll() seems to do this.
        // https://stackoverflow.com/questions/42482705/best-way-to-flatten-an-array-inside-an-rxjs-observable
        concatAll(),
        shareReplay({bufferSize: replayCacheSize, refCount: false}),
      );

  }

  submitVyneQlQueryStreaming(query: string, clientQueryId: string, resultMode: ResultMode = ResultMode.SIMPLE, replayCacheSize = 500): Observable<StreamingQueryMessage> {
    const url = encodeURI(`${environment.queryServiceUrl}/api/vyneql?resultMode=${resultMode}&clientQueryId=${clientQueryId}&query=${query}`);
    return this.sse.getEventStream<ValueWithTypeName>(
      url
    ).pipe(
      catchError((err, caught) => {
        const failure: FailedSearchResponse = {
          message: err,
          remoteCalls: [],
          responseStatus: ResponseStatus.ERROR,
          clientQueryId: null,
          queryResponseId: null
        };
        return of(failure);
      }),
      shareReplay(replayCacheSize)
    );
  }

  getQueryResults(queryId: string, limit: number = 100): Observable<ValueWithTypeName> {
    const url = encodeURI(`${environment.queryServiceUrl}/api/query/history/${queryId}/results?limit=${limit}`);
    return this.sse.getEventStream<ValueWithTypeName>(
      url
    ).pipe(
      shareReplay(limit)
    );
  }

  getHistory(): Observable<QueryHistorySummary[]> {
    return this.http.get<QueryHistorySummary[]>(`${environment.queryServiceUrl}/api/query/history`, this.httpOptions);
  }

  getQueryResultNodeDetail(queryId: string, rowValueId: number, attributePath: string): Observable<QueryResultNodeDetail> {
    return this.http.get<QueryResultNodeDetail>(
      `${environment.queryServiceUrl}/api/query/history/${queryId}/dataSource/${rowValueId}/${encodeURI(attributePath)}`, this.httpOptions
    );
  }

  getQueryResultNodeDetailFromClientId(clientQueryId: string, rowValueId: number, attributePath: string): Observable<QueryResultNodeDetail> {
    return this.http.get<QueryResultNodeDetail>(
      `${environment.queryServiceUrl}/api/query/history/clientId/${clientQueryId}/dataSource/${rowValueId}/${encodeURI(attributePath)}`, this.httpOptions
    );
  }


  getQueryProfileFromClientId(clientQueryId: string): Observable<QueryProfileData> {
    return this.http.get<QueryProfileData>(`${environment.queryServiceUrl}/api/query/history/clientId/${clientQueryId}/profile`, this.httpOptions)
      .pipe(
        shareReplay(1),
        map(profileData => this.parseRemoteCallTimestampsAsDates(profileData))
      ) // This observable is shared
      ;
  }

  getQueryProfile(queryId: string): Observable<QueryProfileData> {
    return this.http.get<QueryProfileData>(`${environment.queryServiceUrl}/api/query/history/${queryId}/profile`, this.httpOptions)
      .pipe(
        shareReplay(1),
        map(profileData => this.parseRemoteCallTimestampsAsDates(profileData))
      ) // This observable is shared
      ;
  }

  private parseRemoteCallTimestampsAsDates(profileData: QueryProfileData): QueryProfileData {
    const remoteCalls = profileData.remoteCalls.map(remoteCall => {
      remoteCall.timestamp = new Date((remoteCall as any).timestamp);
      return remoteCall;
    });
    profileData.remoteCalls = remoteCalls.sort((a, b) => {
      switch (true) {
        case a.timestamp.getTime() < b.timestamp.getTime() :
          return -1;
        case a.timestamp.getTime() > b.timestamp.getTime() :
          return 1;
        default:
          return 0;
      }
    });
    return profileData;
  }

  invokeOperation(serviceName: string, operationName: string, parameters: { [index: string]: Fact }): Observable<TypedInstance> {
    return this.http.post<TypedInstance>(`${environment.queryServiceUrl}/api/services/${serviceName}/${operationName}`, parameters, this.httpOptions);
  }

  cancelQuery(queryId: string): Observable<void> {
    return this.http.delete<void>(`${environment.queryServiceUrl}/api/query/active/${queryId}`, this.httpOptions);
  }

  cancelQueryByClientQueryId(clientQueryId: string): Observable<void> {
    return this.http.delete<void>(`${environment.queryServiceUrl}/api/query/active/clientId/${clientQueryId}`, this.httpOptions);
  }

  getRemoteCallResponse(remoteCallId: string): Observable<string> {
    return this.http.get<string>(`${environment.queryServiceUrl}/api/query/history/calls/${remoteCallId}`);
  }

  getLineageRecord(dataSourceId: string): Observable<LineageRecord> {
    return this.http.get<LineageRecord>(`${environment.queryServiceUrl}/api/query/history/dataSource/${dataSourceId}`);
  }

  getHistorySummaryFromClientId(clientQueryId: string): Observable<QueryHistorySummary> {
    return this.http.get<QueryHistorySummary>(`${environment.queryServiceUrl}/api/query/history/summary/clientId/${clientQueryId}`);
  }

  getQuerySankeyChartData(queryId: string): Observable<QuerySankeyChartRow[]> {
    return this.http.get<QuerySankeyChartRow[]>(`${environment.queryServiceUrl}/api/query/history/${queryId}/sankey`);
  }

  getQuerySankeyChartDataFromClientId(clientQueryId: string): Observable<QuerySankeyChartRow[]> {
    return this.http.get<QuerySankeyChartRow[]>(`${environment.queryServiceUrl}/api/query/history/clientId/${clientQueryId}/sankey`);
  }


}

export interface LineageRecord {
  dataSource: DataSource;
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
  constructor(readonly typeName: string, readonly value: any) {
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
  UNKNOWN = 'UNKNOWN',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  // Ie., the query didn't error, but not everything was resolved
  INCOMPLETE = 'INCOMPLETE',
  ERROR = 'ERROR',
  CANCELLED = 'CANCELLED'
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
  remoteCallId: string;
  service: string;
  serviceDisplayName: string;
  address: string;
  operation: string;
  responseTypeName: string;
  responseTypeDisplayName: string;
  method: string;
  requestBody: any;
  resultCode: number;
  durationMs: number;
  response: any;
  operationQualifiedName: string;
  timestamp: Date;
}

export interface QueryProfileData {
  id: string;
  duration: number;
  remoteCalls: RemoteCall[];
  operationStats: RemoteOperationPerformanceStats[];
  queryLineageData: QuerySankeyChartRow[];
}

export interface RemoteOperationPerformanceStats {
  operationQualifiedName: string;
  serviceName: string;
  operationName: string;
  callsInitiated: number;
  averageTimeToFirstResponse: number;
  totalWaitTime: number | null;
  responseCodes: ResponseCodeCountMap;
}

export type ResponseCodeCountMap = {
  [key in ResponseCodeGroup]: number;
};

export enum ResponseCodeGroup {
  'HTTP_2XX' = 'HTTP_2XX',
  'HTTP_3XX' = 'HTTP_3XX',
  'HTTP_4XX' = 'HTTP_4XX',
  'HTTP_5XX' = 'HTTP_5XX',
  'UNKNOWN' = 'UNKNOWN'
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
  durationMs: number | null;
  recordCount: number;
  errorMessage: string | null;
}


export function randomId(): string {
  return nanoid();
}

export type SankeyNodeType = 'QualifiedName' |
  'AttributeName' |
  'Expression' |
  'ExpressionInput' |
  'ProvidedInput';

export interface QuerySankeyChartRow {
  queryId: string;
  sourceNodeType: SankeyNodeType;
  sourceNode: string;
  targetNodeType: SankeyNodeType;
  targetNode: string;
  count: number;
  id: number;
}


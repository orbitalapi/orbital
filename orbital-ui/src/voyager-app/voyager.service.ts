import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {CompilationMessage, QualifiedName, Schema} from 'src/app/services/schema';
import {environment} from 'src/voyager-app/environments/environment';
import {map} from 'rxjs/operators';
import {StubQueryMessage} from "../app/services/query.service";

export enum SubscriptionResult {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  ALREADY_SUBSCRIBED = 'ALREADY_SUBSCRIBED',
  UNKNOWN = 'UNKNOWN'
}

@Injectable({
  providedIn: 'root'
})
export class VoyagerService {
  constructor(private httpClient: HttpClient) {
  }


  parse(source: string): Observable<ParsedSchema> {
    return this.httpClient.post<ParsedSchema>(`${environment.serverUrl}/api/schema/parse`, source);
  }

  subscribeToEmails(subscribeDetails: SubscribeDetails): Observable<SubscriptionResponse> {
    return this.httpClient.post<SubscriptionResponse>(`${environment.serverUrl}/api/subscribe`, subscribeDetails, {observe: 'response'})
      .pipe(
        map(response => response.body)
      );
  }

  getShareUrl(source: string): Observable<SharedSchemaResponse> {
    return this.httpClient.post<SharedSchemaResponse>(`${environment.serverUrl}/api/schema/share`, source);
  }

  loadSharedSchema(slug: string): Observable<StubQueryMessage> {
    return this.httpClient.get<StubQueryMessage>(`${environment.serverUrl}/api/schema/share/${slug}`)
  }

  runQuery(message: StubQueryMessage): Observable<any> {
    return this.httpClient.post(`${environment.serverUrl}/api/query`, message)
  }

  parseQuery(query: StubQueryMessage): Observable<TaxiQlQuery> {
    return this.httpClient.post<TaxiQlQuery>(`${environment.serverUrl}/api/query/parse`, query);
  }
}

export interface SharedSchemaResponse {
  uri: string;
  id: string;
}

export interface SubscriptionResponse {
  result: SubscriptionResult;
}

export interface ParsedSchema {
  schema: Schema;
  messages: CompilationMessage[];
  hasErrors: boolean;
}

export interface SubscribeDetails {
  email: string;
  otherCommsConsent: boolean;
}

export interface TaxiQlQuery {
  name: QualifiedName;
  facts: Parameter[];
  parameters: Parameter[];
  // other things omitted till we need 'em
}

export interface Parameter {
  name: string;
  value: any;
}

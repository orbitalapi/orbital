import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CompilationMessage, Schema } from 'src/app/services/schema';
import { environment } from 'src/taxi-playground-app/environments/environment';
import { map } from 'rxjs/operators';

export enum SubscriptionResult {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  ALREADY_SUBSCRIBED = 'ALREADY_SUBSCRIBED',
  UNKNOWN = 'UNKNOWN'
}

@Injectable({
  providedIn: 'root'
})
export class TaxiPlaygroundService {
  constructor(private httpClient: HttpClient) {
  }

  parse(source: string): Observable<ParsedSchema> {
    return this.httpClient.post<ParsedSchema>(`${environment.serverUrl}/api/schema/parse`, source);
  }

  subscribeToEmails(subscribeDetails: SubscribeDetails): Observable<SubscriptionResponse> {
    return this.httpClient.post<SubscriptionResponse>(`${environment.serverUrl}/api/subscribe`, subscribeDetails, { observe: 'response' })
      .pipe(
        map(response => response.body)
      );
  }

  getShareUrl(source: string): Observable<SharedSchemaResponse> {
    return this.httpClient.post<SharedSchemaResponse>(`${environment.serverUrl}/api/schema/share`, source);
  }

  loadSharedSchema(slug: string): Observable<string> {
    return this.httpClient.get(`${environment.serverUrl}/api/schema/share/${slug}`, { responseType: 'text' })
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

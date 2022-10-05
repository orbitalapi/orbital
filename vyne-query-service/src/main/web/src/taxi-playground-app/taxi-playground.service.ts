import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CompilationMessage, Schema } from 'src/app/services/schema';
import { environment } from 'src/taxi-playground-app/environments/environment';
import { map } from 'rxjs/operators';

export enum SubscribeSuccess {
  SUCCESS = "SUCCESS",
  FAILED = "FAILED",
  UNKNOWN = "UNKNOWN"
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

  subscribeToEmails(subscribeDetails: SubscribeDetails): Observable<SubscribeSuccess> {
    return this.httpClient.post<HttpResponse<null>>(`${environment.serverUrl}/api/subscribe`, subscribeDetails, { observe: 'response'})
    .pipe(
      map( response => response.status == 200 ? SubscribeSuccess.SUCCESS : SubscribeSuccess.FAILED)
    );
  }
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

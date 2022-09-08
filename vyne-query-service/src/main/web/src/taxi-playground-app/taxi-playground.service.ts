import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CompilationMessage, Schema } from 'src/app/services/schema';
import { environment } from 'src/taxi-playground-app/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TaxiPlaygroundService {
  constructor(private httpClient: HttpClient) {
  }

  parse(source: string): Observable<ParsedSchema> {
    return this.httpClient.post<ParsedSchema>(`${environment.serverUrl}/api/schema/parse`, source);
  }
}

export interface ParsedSchema {
  schema: Schema;
  messages: CompilationMessage[];
  hasErrors: boolean;
}

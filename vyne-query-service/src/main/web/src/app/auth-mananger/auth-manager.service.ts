import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {VyneServicesModule} from '../services/vyne-services.module';
import {Observable} from 'rxjs/internal/Observable';
import {environment} from 'src/environments/environment';

export interface AuthToken {
  tokenType: AuthTokenType;
  value: string;
  paramName: string;
  valuePrefix: string | null;
}

export interface NoCredentialsAuthToken {
  serviceName: string;
  tokenType: AuthTokenType;
}

export enum AuthTokenType {
  Header = "Header",
  QueryParam = "QueryParam",
  Cookie = "Cookie"
}

export function authTokenTypeDisplayName(tokenType: AuthTokenType): string {
  switch (tokenType) {
    case AuthTokenType.Header:
      return 'Request Header Value';
    case AuthTokenType.QueryParam:
      return "Request Query Parameter Key-Value"
    case AuthTokenType.Cookie:
      return "Cookie Based Token"

  }
}

@Injectable({
  providedIn: VyneServicesModule
})
export class AuthManagerService {
  private tokensApiEndpoint = `${environment.queryServiceUrl}/api/tokens`;
  constructor(private http: HttpClient) {
  }

  getAllTokens(): Observable<NoCredentialsAuthToken[]> {
    return this.http.get<NoCredentialsAuthToken[]>(`${this.tokensApiEndpoint}`);
  }

  saveToken(serviceName: string, token: AuthToken): Observable<NoCredentialsAuthToken> {
    return this.http.post<NoCredentialsAuthToken>(`${this.tokensApiEndpoint}/service/${serviceName}`, token);
  }

  deleteToken(serviceName: string): Observable<any> {
    return this.http.delete(`${this.tokensApiEndpoint}/service/${serviceName}`);
  }
}

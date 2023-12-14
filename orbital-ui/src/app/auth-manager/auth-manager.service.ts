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

export type AuthSchemeKind = 'Basic' | 'HttpHeader' | 'QueryParam' | 'Cookie' | 'OAuth2';

export interface AuthScheme {
  type: AuthSchemeKind
}

export interface BasicAuth extends AuthScheme {
  type: 'Basic';
  username: string;
  password: string;
}

export interface HttpHeader extends AuthScheme {
  type: 'HttpHeader';
  prefix: string;
  headerName: string;
}

export interface QueryParam extends AuthScheme {
  type: 'QueryParam'
  parameterName: string;
  value: string;
}

export interface Cookie extends AuthScheme {
  type: 'Cookie';
  cookieName: string;
  value: string;
}

export enum AuthTokenType {
  Header = "Header",
  QueryParam = "QueryParam",
  Cookie = "Cookie"
}


export type AuthTokenMap = { [index: string]: AuthScheme }

@Injectable({
  providedIn: VyneServicesModule
})
export class AuthManagerService {
  private tokensApiEndpoint = `${environment.serverUrl}/api/tokens`;

  constructor(private http: HttpClient) {
  }

  getAllTokens(): Observable<AuthTokenMap> {
    return this.http.get<AuthTokenMap>(`${this.tokensApiEndpoint}`);
  }

  saveToken(serviceName: string, token: AuthToken): Observable<NoCredentialsAuthToken> {
    return this.http.post<NoCredentialsAuthToken>(`${this.tokensApiEndpoint}/service/${serviceName}`, token);
  }

  deleteToken(serviceName: string): Observable<any> {
    return this.http.delete(`${this.tokensApiEndpoint}/service/${serviceName}`);
  }
}

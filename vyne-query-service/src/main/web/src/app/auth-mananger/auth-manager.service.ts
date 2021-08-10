import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {VyneServicesModule} from '../services/vyne-services.module';
import {Observable} from 'rxjs/internal/Observable';
import {environment} from 'src/environments/environment';

export interface AuthToken {
  tokenType: AuthTokenType;
  value: string;
}

export interface NoCredentialsAuthToken {
  serviceName: string;
  tokenType: AuthTokenType;
}

export type AuthTokenType = 'AuthorizationBearerHeader';

export function authTokenTypeDisplayName(tokenType: AuthTokenType): string {
  switch (tokenType) {
    case 'AuthorizationBearerHeader':
      return 'Authorization bearer token';
  }
}

@Injectable({
  providedIn: VyneServicesModule
})
export class AuthManagerService {
  constructor(private http: HttpClient) {
  }

  getAllTokens(): Observable<NoCredentialsAuthToken[]> {
    return this.http.get<NoCredentialsAuthToken[]>(`${environment.queryServiceUrl}/api/tokens`);
  }

  saveToken(serviceName: string, token: AuthToken): Observable<NoCredentialsAuthToken> {
    return this.http.post<NoCredentialsAuthToken>(`${environment.queryServiceUrl}/api/tokens/service/${serviceName}`, token);
  }

  deleteToken(serviceName: string): Observable<any> {
    return this.http.delete(`${environment.queryServiceUrl}/api/tokens/service/${serviceName}`);
  }
}

import { Inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';

import { map, shareReplay } from 'rxjs/operators';
import { ENVIRONMENT, Environment } from 'src/app/services/environment';

@Injectable({
  providedIn: 'root'
})
export class UserInfoService {

  readonly userInfo$: BehaviorSubject<VyneUser> = new BehaviorSubject<VyneUser>(null);

  constructor(private httpClient: HttpClient,
              @Inject(ENVIRONMENT) private environment: Environment
  ) {
  }

  getAllUsers(): Observable<VyneUser[]> {
    return this.httpClient.get<VyneUser[]>(`${this.environment.serverUrl}/api/users`);
  }

  /**
   * Requests the current Vyne user.
   * Most importantly, this also sets a cookie with the current auth token,
   * so that SSE/EventSource and Websocket requests (which don't support auth headers)
   * have the auth propagated
   */
  getUserInfo(refresh: boolean = false, accessToken: string | null = null): Observable<VyneUser> {
    if (refresh) {
      console.log('fetching user data');
      if (accessToken) {
        let header = 'Bearer ' + accessToken;
        let headers = new HttpHeaders().set('Authorization', header);
        return this.httpClient.get<VyneUser>(`${this.environment.serverUrl}/api/user`, { headers: headers })
          .pipe(map(vyneUser => {
            this.userInfo$.next(vyneUser);
            return this.userInfo$.getValue();
          }));
      } else {
        return this.httpClient.get<VyneUser>(`${this.environment.serverUrl}/api/user`)
          .pipe(map(vyneUser => {
            this.userInfo$.next(vyneUser);
            return this.userInfo$.getValue();
          }));
      }
    }
    return this.userInfo$;
  }

  updateUserInfo(vyneUser: VyneUser) {
    this.userInfo$.next(vyneUser);
  }
}

export interface VyneUser {
  userId: string;
  username: string;
  email: string;
  profileUrl: string | null;
  name: string | null;
  grantedAuthorities: VynePrivileges[];
  isAuthenticated: boolean;
}

export const EmptyVyneUser: VyneUser = {
  userId: '',
  username: '',
  email: '',
  profileUrl: null,
  name: null,
  grantedAuthorities: [],
  isAuthenticated: false
}

export enum VynePrivileges {
  RunQuery = 'RunQuery',
  CancelQuery = 'CancelQuery',
  ViewQueryHistory = 'ViewQueryHistory',
  ViewHistoricQueryResults = 'ViewHistoricQueryResults',
  BrowseCatalog = 'BrowseCatalog',
  BrowseSchema = 'BrowseSchema',
  EditSchema = 'EditSchema',
  ViewCaskDefinitions = 'ViewCaskDefinitions',
  EditCaskDefinitions = 'EditCaskDefinitions',
  ViewPipelines = 'ViewPipelines',
  EditPipelines = 'EditPipelines',
  ViewAuthenticationTokens = 'ViewAuthenticationTokens',
  EditAuthenticationTokens = 'EditAuthenticationTokens',
  ViewConnections = 'ViewConnections',
  EditConnections = 'EditConnections',
  ViewUsers = 'ViewUsers',
  EditUsers = 'EditUsers'
}

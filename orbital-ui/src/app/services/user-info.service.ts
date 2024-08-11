import {Inject, Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {BehaviorSubject, Observable, of, throwError} from 'rxjs';

import {catchError, map, shareReplay} from 'rxjs/operators';
import {ENVIRONMENT, Environment} from 'src/app/services/environment';
import {Router} from "@angular/router";

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
      const headers = accessToken ?
        new HttpHeaders().set('Authorization', 'Bearer ' + accessToken)
        : undefined;
      return this.httpClient.get<VyneUser>(`${this.environment.serverUrl}/api/user`, {headers})
        .pipe(
          catchError(error => {
            this.userInfo$.next(null);
            return throwError(() => error)
          }),
          map(vyneUser => {
            this.userInfo$.next(vyneUser);
            return this.userInfo$.getValue();
          }),

        );
    } else {
      return this.userInfo$;
    }

  }

  updateUserInfo(vyneUser: VyneUser) {
    this.userInfo$.next(vyneUser);
  }
}

export type AuthenticationType = 'Oidc' | 'Saml'

export interface VyneUser {
  userId: string;
  username: string;
  email: string;
  profileUrl: string | null;
  name: string | null;
  grantedAuthorities: VynePrivileges[];
  isAuthenticated: boolean;
  authenticationType: AuthenticationType | null;
}

export const EmptyVyneUser: VyneUser = {
  userId: '',
  username: '',
  email: '',
  profileUrl: null,
  name: null,
  grantedAuthorities: [],
  isAuthenticated: false,
  authenticationType: null
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
  EditUsers = 'EditUsers',
  CreateWorkspace = 'CreateWorkspace',
  ViewMetrics = 'ViewMetrics'
}

// Below, we want to expose the keys of VynePrivileges
// as strings. We have to do that in two steps.
// First, use typeof  to access the type of the enum for
// reflection purposes.
type PrivilegeType = typeof VynePrivileges;
// Then expose the keys.
// These are stringly typed.
// This allows intellisense in things like templates, where
// enums aren't exposed
export type Privilege = keyof PrivilegeType;

import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, Observable, of} from 'rxjs';

import {environment} from 'src/environments/environment';
import {map, shareReplay} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class UserInfoService {

  readonly userInfo$: BehaviorSubject<VyneUser> = new BehaviorSubject<VyneUser>(EmptyVyneUser);

  constructor(private httpClient: HttpClient) { }

  getAllUsers(): Observable<VyneUser[]> {
    return this.httpClient.get<VyneUser[]>(`${environment.queryServiceUrl}/api/users`);
  }

  /**
   * Requests the current Vyne user.
   * Most importantly, this also sets a cookie with the current auth token,
   * so that SSE/EventSource and Websocket requests (which don't support auth headers)
   * have the auth propagated
   */
  getUserInfo(refresh: boolean = false): Observable<VyneUser> {
    if (refresh || !this.userInfo$) {
      this.httpClient.get<VyneUser>(`${environment.queryServiceUrl}/api/user`)
        .pipe(map(user => this.userInfo$.next(user)));
    }
    return this.userInfo$;
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
  userId: "",
  username: "",
  email: "",
  profileUrl: null,
  name: null,
  grantedAuthorities: [],
  isAuthenticated: false
}

export enum VynePrivileges {
  RunQuery = "RunQuery",
  CancelQuery = "CancelQuery",
  ViewQueryHistory = "ViewQueryHistory",
  ViewHistoricQueryResults = "ViewHistoricQueryResults",
  BrowseCatalog = "BrowseCatalog",
  BrowseSchema = "BrowseSchema",
  EditSchema = "EditSchema",
  ViewCaskDefinitions = "ViewCaskDefinitions",
  EditCaskDefinitions = "EditCaskDefinitions",
  ViewPipelines = "ViewPipelines",
  EditPipelines = "EditPipelines",
  ViewAuthenticationTokens = "ViewAuthenticationTokens",
  EditAuthenticationTokens = "EditAuthenticationTokens",
  ViewConnections = "ViewConnections",
  EditConnections = "EditConnections",
  ViewUsers = "ViewUsers",
  EditUsers = "EditUsers"
}

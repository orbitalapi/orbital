import {Inject, Injectable} from "@angular/core";
import {VyneServicesModule} from "./vyne-services.module";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {SseEventSourceService} from "./sse-event-source.service";
import {Environment, ENVIRONMENT} from "./environment";
import {WebsocketService} from "./websocket.service";
import {Observable} from "rxjs/internal/Observable";
import {merge, ReplaySubject, Subject} from "rxjs";
import {LocalStorageSubject} from "./local-storage-subject";
import {filter, flatMap, map, mergeMap, switchMap, tap} from "rxjs/operators";

@Injectable({
  providedIn: VyneServicesModule
})

export class WorkspacesService {

  private activeWorkspaceId$ = new LocalStorageSubject<number>('last-workspace-id');
  organisationId = "orgId"; // TODO
  private workspaceMemberships$ = new ReplaySubject<WorkspaceMembershipDto[]>(1);
  readonly activeWorkspaceMembership$: Observable<WorkspaceMembershipDto>

  constructor(private http: HttpClient,
              @Inject(ENVIRONMENT) private environment: Environment,
  ) {
    this.refreshMemberships().subscribe()

    this.activeWorkspaceMembership$ = this.activeWorkspaceId$.observable().pipe(
      mergeMap(activeWorkspaceId => {
          return this.workspaceMemberships$.pipe(
            map(memberships => memberships.find(membership => membership.workspace.id === activeWorkspaceId))
          )
        }
      )
    )
  }

  switchWorkspace(id: number) {
    this.activeWorkspaceId$.setValue(id)
  }

  refreshMemberships(): Observable<WorkspaceMembershipDto[]> {
    return this.loadCurrentMemberships().pipe(
      tap(next => this.workspaceMemberships$.next(next))
    )
  }

  createWorkspace(name: string, makeActive: boolean): Observable<Workspace> {
    const request = {
      workspaceName: name
    }
    return this.http.post<Workspace>(`${this.environment.serverUrl}/api/workspaces/${this.organisationId}`, request)
      .pipe(
        tap((newWorkspace) => {
          this.refreshMemberships().subscribe(() => {
            if (makeActive) {
              this.switchWorkspace(newWorkspace.id)
            }
          })
        })
      )
  }

  getCurrentWorkspaceMemberships(): Observable<WorkspaceMembershipDto[]> {
    return this.workspaceMemberships$.asObservable()
  }

  private loadCurrentMemberships(): Observable<WorkspaceMembershipDto[]> {
    return this.http.get<WorkspaceMembershipDto[]>(`${this.environment.serverUrl}/api/workspaces/${this.organisationId}`)
  }
}

export interface WorkspaceMembershipDto {
  workspace: Workspace;
  roles: string[];
}

export interface Workspace {
  id: number;
  name: string;
}

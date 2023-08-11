import {Inject, Injectable} from "@angular/core";
import {VyneServicesModule} from "./vyne-services.module";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {SseEventSourceService} from "./sse-event-source.service";
import {Environment, ENVIRONMENT} from "./environment";
import {WebsocketService} from "./websocket.service";
import {Observable} from "rxjs/internal/Observable";
import {ReplaySubject, Subject} from "rxjs";

@Injectable({
    providedIn: VyneServicesModule
})

export class WorkspacesService {

    activeWorkspace$ = new ReplaySubject<Workspace>(1)
    organisationId = "orgId"; // TODO
    constructor(private http: HttpClient,
                @Inject(ENVIRONMENT) private environment: Environment,
    ) {
    }

    createWorkspace(name: string): Observable<Workspace> {
        const request = {
            workspaceName: name
        }
        return this.http.post<Workspace>(`${this.environment.serverUrl}/api/${this.organisationId}/workspaces`, request)
    }

    getCurrentWorkspaceMemberships(): Observable<WorkspaceMembershipDto[]> {

        return this.http.get<WorkspaceMembershipDto[]>(`${this.environment.serverUrl}/api/${this.organisationId}/workspaces`)
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

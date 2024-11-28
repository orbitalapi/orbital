import {Inject, Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Environment, ENVIRONMENT} from "./environment";
import {concatWith, Observable} from "rxjs";
import {WebsocketService} from "./websocket.service";
import {shareReplay} from "rxjs/operators";
import {SchemaUpdatedNotification} from "./schema-notification.service";
import {ConnectionStatus} from "../db-connection-editor/db-importer.service";

@Injectable({
  providedIn: 'root',
})
export class StubsApiService {
  private readonly stubStateStream$: Observable<NebulaStacksResponse>;
  private serverStatus$: Observable<ConnectionStatus>;

  constructor(private http: HttpClient,
              private websocketService: WebsocketService,
              @Inject(ENVIRONMENT) private environment: Environment,
  ) {
    // Initial state over http,
    // followed by stream of updates over websocket
    this.stubStateStream$ = this.getStubStates()
      .pipe(
        concatWith(websocketService.connect<NebulaStacksResponse>('/api/stubs/updates')),
        shareReplay(1)
      );

    this.serverStatus$ = this.websocketService.connect<ConnectionStatus>('/api/stubs/server-status')
      .pipe(
        shareReplay(1)
      )
  }


  getStubStates(): Observable<NebulaStacksResponse> {
    return this.http.get<NebulaStacksResponse>(`${this.environment.serverUrl}/api/stubs`)
  }

  getStubStateStream(): Observable<NebulaStacksResponse> {
    return this.stubStateStream$
  }

  getStubServerStatus(): Observable<ConnectionStatus> {
    return this.serverStatus$;
  }
}

export type StackName = string
export type ComponentType = string
export type ComponentName = string
export type EnvVarKey = string
export type EnvVarValue = string

export interface NebulaStacksResponse {
  stacks: { [key: StackName]: ComponentInfoWithState[] };

  // A list of env variables for each stack, grouped by the stack name
  // eg: Map<String,Map<String,String>>
  environmentVariables: { [key: StackName]: { [key: ComponentType]: { [key: EnvVarKey]: EnvVarValue } } };
  hasError: boolean;
  error: string | null;
  hasPendingUpdates: boolean;
}

export interface ComponentInfo {
  container: ContainerInfo;
  componentConfig: { [key: string]: any };
  type: ComponentType;
  name: ComponentName;
  id: string;

}

export interface ComponentInfoWithState {
  name: ComponentName,
  type: ComponentType,
  id: String,
  state: ComponentLifecycleEvent
  componentInfo: ComponentInfo
}

export type ComponentState = 'NotStarted' | 'Starting' | 'Running' | 'Stopping' | 'Stopped' | 'Failed';

export interface ComponentLifecycleEvent {
  state: ComponentState
  message?: string // on LifecycleEventWithMessage
}

export interface ContainerInfo {
  containerId: string;
  imageName: string;
  containerName: string

}

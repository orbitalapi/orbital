import {Inject, Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Environment, ENVIRONMENT} from "./environment";
import {Observable} from "rxjs";

@Injectable({
  providedIn: 'root',
})
export class StubsApiService {
  constructor(private http: HttpClient,
              @Inject(ENVIRONMENT) private environment: Environment,
  ) {
  }

  getStubStates(): Observable<NebulaStacksResponse> {
    return this.http.get<NebulaStacksResponse>(`${this.environment.serverUrl}/api/stubs`)
  }
}

export type StackName = string
export type ComponentType = string
export type EnvVarKey = string
export type EnvVarValue = string

export interface NebulaStacksResponse {
  // eg: Map<String,Map<String,ComponentInfo>>
  stacks: { [key: StackName]: { [key: string]: ComponentInfo } };

  // A list of env variables for each stack, grouped by the stack name
  // eg: Map<String,Map<String,String>>
  environmentVariables: { [key: StackName]: { [key: ComponentType]: { [key: EnvVarKey]  : EnvVarValue } } };
  hasError: boolean;
  error: string | null;
  hasPendingUpdates: boolean;
}

export interface ComponentInfo {
  container: ContainerInfo;
  componentConfig: { [key: string]: any };
}

export interface ContainerInfo {
  containerId: string;
  imageName: string;
  containerName: string

}

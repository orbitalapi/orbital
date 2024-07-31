import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AppInfoService {

  constructor(private httpClient: HttpClient) {
  }

  private config: AppConfig;

  getAppInfo(actuatorEndPoint: string = '/api/actuator'): Observable<AppInfo> {
    return this.httpClient.get<AppInfo>(`${environment.serverUrl}${actuatorEndPoint}/info`);
  }

  getConfig(): Observable<AppConfig> {
    if (this.config) {
      return of(this.config);
    } else {
      const observable = this.httpClient.get<AppConfig>(`${environment.serverUrl}/api/config`);
      observable.subscribe(result => {
        this.config = result;
      });
      return observable;
    }
  }

}

// Called ConfigSummary in the server code
export interface AppConfig {
  analytics: {
    maxPayloadSizeInBytes: number
    persistRemoteCallResponses: boolean
    pageSize: number
    persistResults: boolean
    maxQueryRecordCount: number
  }
  licenseStatus: {
    isLicensed: boolean;
    expiresOn: Date
  }
  actuatorPath: string;
  pipelineConfig: PipelineConfig;
  featureToggles: FeatureToggles;
  custom: {[index:string]:any}
}

export interface FeatureToggles {
  copilotEnabled: boolean;
  workspacesEnabled: boolean;
  policiesEnabled: boolean;
  queryPlanModeEnabled: boolean;
  serviceLineageDiagramsEnabled: boolean;
  copyAsCodeEnabled: boolean
}
export interface PipelineConfig {
  kibanaUrl: string;
  logsIndex: string;
}

export class AppInfo {
  git: {
    commit: {
      time: string
      id: string
    }
  };
  build: {
    baseVersion: string
    version: string
    buildNumber: string
    time: string
  };
}

export class GitInfo {

}

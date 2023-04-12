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

  private config: QueryServiceConfig;

  getAppInfo(actuatorEndPoint: string = '/api/actuator'): Observable<AppInfo> {
    return this.httpClient.get<AppInfo>(`${environment.serverUrl}${actuatorEndPoint}/info`);
  }

  getConfig(): Observable<QueryServiceConfig> {
    if (this.config) {
      return of(this.config);
    } else {
      const observable = this.httpClient.get<QueryServiceConfig>(`${environment.serverUrl}/api/config`);
      observable.subscribe(result => {
        this.config = result;
      });
      return observable;
    }


  }

}

export interface QueryServiceConfig {
  analytics: {
    maxPayloadSizeInBytes: number
    persistRemoteCallResponses: boolean
    pageSize: number,
    persistResults: boolean
  };
  licenseStatus: {
    isLicensed: boolean;
    expiresOn: Date
  }
  actuatorPath: string;
  pipelineConfig: PipelineConfig;
  featureToggles: FeatureToggles;
}

export interface FeatureToggles {
  chatGptEnabled: boolean;
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

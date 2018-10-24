import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs/internal/Observable";

import {environment} from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AppInfoService {

  constructor(private httpClient: HttpClient) {
  }

  getAppInfo(): Observable<AppInfo> {
    return this.httpClient.get<AppInfo>(`${environment.queryServiceUrl}/actuator/info`)
  }

}

export class AppInfo {
  git: {
    commit: {
      time: string
      id: string
    }
  };
  build: {
    version: string
    time: string
  };
}

export class GitInfo {

}

import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of} from 'rxjs';

import {environment} from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserInfoService {

  constructor(private httpClient: HttpClient) {
  }

  getUserInfo(): Observable<UserInfo> {
    return this.httpClient.get<UserInfo>(`${environment.queryServiceUrl}/api/user`);
  }
}

export interface UserInfo {
  userName: string | null;
}

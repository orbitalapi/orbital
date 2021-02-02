import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of} from 'rxjs';

import {environment} from 'src/environments/environment';
import {shareReplay} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class UserInfoService {

  private readonly userInfo$: Observable<VyneUser>;

  constructor(private httpClient: HttpClient) {
    this.userInfo$ = this.httpClient.get<VyneUser>(`${environment.queryServiceUrl}/api/user`)
      .pipe(
        shareReplay({bufferSize: 1, refCount: true})
      );
  }


  getUserInfo(): Observable<VyneUser> {
    return this.userInfo$;
  }
}

export interface VyneUser {
  userId: string;
  username: string;
  email: string;
  profileUrl: string | null;
  name: string | null;
}

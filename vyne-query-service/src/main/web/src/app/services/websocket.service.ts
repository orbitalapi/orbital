import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/index';
import {environment} from '../../environments/environment';
import {delay, map, retryWhen, switchMap} from 'rxjs/operators';
import {WebSocketSubject} from 'rxjs/internal-compatibility';
import {webSocket} from 'rxjs/webSocket';
import {of} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private connection$: WebSocketSubject<any>;
  RETRY_SECONDS = 10;

  connect(path: string): Observable<any> {
    return of(environment.queryServiceUrl).pipe(
      // https becomes wws, http becomes ws
      map(apiUrl => {
        if (apiUrl.startsWith('http')) {
          return apiUrl.replace(/^http/, 'ws') + path;
        } else {
          // Handle urls that omit the scheme (ie., defer to the page protocol)
          const protocol = document.location.protocol === 'https:' ? 'wss:' : 'ws:';
          return protocol + apiUrl + path;
        }
      }),
      switchMap(wsUrl => {
        if (this.connection$) {
          return this.connection$;
        } else {
          this.connection$ = webSocket(wsUrl);
          return this.connection$;
        }
      }),
      retryWhen((errors) => errors.pipe(delay(this.RETRY_SECONDS)))
    );
  }
}

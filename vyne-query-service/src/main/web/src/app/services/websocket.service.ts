import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { delay, map, retryWhen, switchMap } from 'rxjs/operators';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private connections = new Map<string, WebSocketSubject<any>>();
  RETRY_SECONDS = 10;

  getWsUrl(path: string): string {
    const apiUrl = environment.queryServiceUrl;
    if (apiUrl.startsWith('http')) {
      return apiUrl.replace(/^http/, 'ws') + path;
    } else {
      // Handle urls that omit the scheme (ie., defer to the page protocol)
      const protocol = document.location.protocol === 'https:' ? 'wss:' : 'ws:';
      return protocol + apiUrl + path;
    }
  }

  connect(path: string): Observable<any> {
    return of(path).pipe(
      // https becomes wws, http becomes ws
      map(apiUrl => this.getWsUrl(path)),
      switchMap((wsUrl: string) => {
        if (this.connections.has(wsUrl)) {
          return this.connections.get(wsUrl);
        } else {
          const connection$ = webSocket(wsUrl);
          this.connections.set(wsUrl, connection$);
          return connection$;
        }
      }),
      retryWhen((errors) => errors.pipe(delay(this.RETRY_SECONDS)))
    );
  }

  static buildWsUrl(appServerUrl: string, path: string): string {
    if (appServerUrl.startsWith('http')) {
      return appServerUrl.replace(/^http/, 'ws') + path;
    } else {
      // Handle urls that omit the scheme (ie., defer to the page protocol)
      const protocol = document.location.protocol === 'https:' ? 'wss:' : 'ws:';
      return protocol + appServerUrl + path;
    }
  }
}

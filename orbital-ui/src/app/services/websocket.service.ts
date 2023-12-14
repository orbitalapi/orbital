import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { delay, retryWhen } from 'rxjs/operators';
import { WebSocketSubject } from 'rxjs/webSocket';
import { webSocket } from 'rxjs/webSocket';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private connections = new Map<string, WebSocketSubject<any>>();
  RETRY_SECONDS = 10;

  getWsUrl(path: string) {
    const apiUrl = environment.serverUrl;
    if (apiUrl.startsWith('http')) {
      return apiUrl.replace(/^http/, 'ws') + path;
    } else {
      // Handle urls that omit the scheme (ie., defer to the page protocol)
      const protocol = document.location.protocol === 'https:' ? 'wss:' : 'ws:';
      return protocol + apiUrl + path;
    }
  }

  /**
   * Returns a websocket that supports sending and receiving,
   * but does not reconnect on failure.
   */
  websocket(path: string): WebSocketSubject<any> {
    return this.getOrBuildWebsocket(this.getWsUrl(path))
  }

  /**
   * Returns a websocket that receives updates, and reconnects on failure.
   * Does not support sending
   */
  connect(path: string): Observable<any> {
    return this.websocket(path)
      .pipe(
        retryWhen((errors) => errors.pipe(delay(this.RETRY_SECONDS)))
      );
  }

  private getOrBuildWebsocket(wsUrl): WebSocketSubject<any> {
    if (this.connections.has(wsUrl)) {
      return this.connections.get(wsUrl);
    } else {
      const connection$ = webSocket(wsUrl);
      this.connections.set(wsUrl, connection$);
      return connection$;
    }
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

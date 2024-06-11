import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/index';
import {filter, map, share, shareReplay} from 'rxjs/operators';
import {IMessage} from '@stomp/stompjs';
import {WebsocketService} from './websocket.service';

@Injectable({
  providedIn: 'root'
})

export class ActiveQueriesNotificationService {

  private readonly activeQueries$: Observable<RunningQueryStatus>;

  constructor(private websocketService: WebsocketService) {
    this.activeQueries$ = this.websocketService.connect<RunningQueryStatus>('/api/query/status')
      .pipe(
        map((message) => {
          // Convert the utc date string received to a js Date object
          const startTimeString = (message as any).startTime as string;
          message.startTime = new Date(Date.parse(startTimeString));
          return message;
        }),
        share()
      );
  }

  getQueryStatusStreamForQueryId(queryId: string): Observable<RunningQueryStatus> {
    return this.activeQueries$
      .pipe(
        filter(message => message.queryId === queryId)
      );
  }

  createActiveQueryNotificationSubscription(): Observable<RunningQueryStatus> {
    return this.activeQueries$;
  }

}

export interface RunningQueryStatus {
  queryId: string;
  taxiQlQuery: string;
  completedProjections: number;
  estimatedProjectionCount: number | null;
  startTime: Date;
  responseTypeName: string;
  running: boolean;
  queryMode: QueryMode;
}

export type QueryMode = 'FIND_ALL' | 'MAP' | 'STREAM' | 'MUTATE'


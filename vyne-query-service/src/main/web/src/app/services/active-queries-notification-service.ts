import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/index';
import {filter, map, shareReplay} from 'rxjs/operators';
import {IMessage} from '@stomp/stompjs';
import {WebsocketService} from './websocket.service';

@Injectable({
  providedIn: 'root'
})

export class ActiveQueriesNotificationService {

  private readonly activeQueries$: Observable<RunningQueryStatus>;

  constructor(private websocketService: WebsocketService) {
    this.activeQueries$ = this.websocketService.connect('/api/query/status')
      .pipe(
        map((message: RunningQueryStatus) => {
          // Convert the utc date string received to a js Date object
          const startTimeString = (message as any).startTime as string;
          message.startTime = new Date(Date.parse(startTimeString));
          return message;
        }),
        shareReplay(10) // buffer size is arbitary, we probably don't need a buffer here any.
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
  vyneQlQuery: string;
  completedProjections: number;
  estimatedProjectionCount: number | null;
  startTime: Date;
  responseTypeName: string;
  running: boolean;
  queryType: QueryType;
}

export type QueryType = 'STREAMING' | 'DETERMINANT';


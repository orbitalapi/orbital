import {Injectable} from '@angular/core';
import {RxStompService} from '@stomp/ng2-stompjs';
import {Observable} from 'rxjs/index';
import {map} from 'rxjs/operators';
import {IMessage} from '@stomp/stompjs';
import {SchemaUpdatedNotification} from './schema-notification.service';
import {TypedInstance, TypeNamedInstance} from './schema';

@Injectable({
  providedIn: 'root'
})
export class ActiveQueriesNotificationService {
  constructor(private rxStompService: RxStompService) {
  }

  getQueryResultStream(queryId: string, consumerId: number): Observable<TypeNamedInstance> {
    this.rxStompService.publish({
      destination: `/app/query/${queryId}/${consumerId}`,
      body: 'Subscribe'
    });
    return this.rxStompService.watch(`/topic/query/${queryId}/${consumerId}`)
      .pipe(map((message: IMessage) => {
        const parsed = JSON.parse(message.body) as TypeNamedInstance;
        return parsed;
      }));
  }

  getQueryStatusStream(queryId: string): Observable<RunningQueryStatus> {
    return this.rxStompService.watch(`/topic/runningQueryUpdates/${queryId}`)
      .pipe(map((message: IMessage) => {
        const parsed = JSON.parse(message.body) as RunningQueryStatus;
        parsed.startTime = new Date(Date.parse((parsed as any).startTime as string));
        return parsed;
      }));
  }

  createActiveQueryNotificationSubscription(): Observable<RunningQueryStatus> {
    return this.rxStompService.watch('/topic/runningQueryUpdates')
      .pipe(map((message: IMessage) => {
        const parsed = JSON.parse(message.body) as RunningQueryStatus;
        parsed.startTime = new Date(Date.parse((parsed as any).startTime as string));
        return parsed;
      }));
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
}

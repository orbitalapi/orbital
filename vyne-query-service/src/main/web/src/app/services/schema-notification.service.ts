import {Injectable} from '@angular/core';
//import {RxStompService} from '@stomp/ng2-stompjs';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
//import {IMessage} from '@stomp/stompjs';

@Injectable({
  providedIn: 'root'
})
export class SchemaNotificationService {

  //constructor(private rxStompService: RxStompService) {
  //}

  //createSchemaNotificationsSubscription(): Observable<SchemaUpdatedNotification> {
  //  return this.rxStompService.watch('/topic/schemaNotifications')
  //      return JSON.parse(message.body) as SchemaUpdatedNotification;
  //    }));
  //}
}

export interface SchemaUpdatedNotification {
  newId: number;
  generation: number;
  invalidSourceCount: number;
}

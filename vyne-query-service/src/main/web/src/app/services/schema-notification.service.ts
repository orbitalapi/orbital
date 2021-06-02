import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {WebsocketService} from './websocket.service';
import {share} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class SchemaNotificationService {
  private readonly schemaUpdates$: Observable<SchemaUpdatedNotification>;

  constructor(private websocketService: WebsocketService) {
    this.schemaUpdates$ = websocketService.connect('/api/schema/updates')
      .pipe(
        share()
      );
  }

  createSchemaNotificationsSubscription(): Observable<SchemaUpdatedNotification> {
    return this.schemaUpdates$;
  }
}

export interface SchemaUpdatedNotification {
  newId: number;
  generation: number;
  invalidSourceCount: number;
}

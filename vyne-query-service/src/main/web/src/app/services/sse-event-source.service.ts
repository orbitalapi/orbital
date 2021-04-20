import {Injectable, NgZone} from '@angular/core';
import {Observable} from 'rxjs/index';
import {isValueWithTypeName, ValueWithTypeName} from './query.service';
import {isNullOrUndefined} from 'util';

@Injectable({
  providedIn: 'root'
})
export class SseEventSourceService {
  constructor(private zone: NgZone) {
  }

  getEventStream<T>(url: string, errorAfterMessageIndicatesClosed: boolean = true): Observable<T> {
    return new Observable<T>(observer => {
      console.log(`Initiating event stream at ${url}`);
      const eventSource = new EventSource(url);
      let messageReceived = false;
      eventSource.onmessage = (event: MessageEvent) => {
        // Note - this was running inside zone.run(), but I removed it as it created race conditions
        // with termination events being processed before all the messages were processed.
        messageReceived = true;

        // Check for errors:
        const payload = JSON.parse(event.data) as T;
        if (isErrorMessage(payload)) {
          observer.error(payload.value);
        } else {
          observer.next(payload);
        }

      };
      eventSource.onerror = (error: Event) => {
        if (messageReceived && errorAfterMessageIndicatesClosed) {
          console.log('Received error event after recieving content - treating this as a close signal');
          // NgZone.run removed from here as above
          observer.complete();
        } else {
          console.log('Received error event' + JSON.stringify(error));
          // NgZone.run removed from here as above
          observer.error(error);
        }
      };
      observer.add(() => {
        console.log(`Closing event stream at ${url}`);
        eventSource.close();
      });
    });


  }
}

function isErrorMessage(event: any): event is ValueWithTypeName {
  return !isNullOrUndefined(event.typeName) &&
    event.typeName === 'vyne.errors.Error';
}

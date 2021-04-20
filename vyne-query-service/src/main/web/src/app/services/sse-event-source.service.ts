import {Injectable, NgZone} from '@angular/core';
import {Observable} from 'rxjs/index';

@Injectable({
  providedIn: 'root'
})
export class SseEventSourceService {
  constructor(private zone: NgZone) {
  }

  getEventSource(url: string, errorAfterMessageIndicatesClosed: boolean = true): Observable<MessageEvent> {
    return new Observable<MessageEvent>(observer => {
      console.log(`Initiating event stream at ${url}`);
      const eventSource = new EventSource(url);
      let messageReceived = false;
      eventSource.onmessage = (event: MessageEvent) => {
        // Note - this was running inside zone.run(), but I removed it as it created race conditions
        // with termination events being processed before all the messages were processed.
        messageReceived = true;
        observer.next(event);
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

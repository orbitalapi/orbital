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
        this.zone.run(() => {
          messageReceived = true;
          observer.next(event);
        });
      };
      eventSource.onerror = (error: Event) => {
        if (messageReceived && errorAfterMessageIndicatesClosed) {
          console.log('Received error event after recieving content - treating this as a close signal')
          this.zone.run(() => observer.complete());
        } else {
          console.log('Received error event' + JSON.stringify(error));
          this.zone.run(() => observer.error(error));
        }
      };
      observer.add(() => {
        console.log(`Closing event stream at ${url}`);
        eventSource.close();
      });
    });


  }
}

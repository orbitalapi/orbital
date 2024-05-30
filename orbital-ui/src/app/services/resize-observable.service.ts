import {Injectable, OnDestroy} from '@angular/core';
import { filter, map, NextObserver, Observable, Subscriber } from "rxjs";
import { debounceTime } from 'rxjs/operators';

// NOTE: this should be Provided by the view that needs it,
//       not at a root level and when the view is destroyed, the
//       service will run it's ngOnDestroy method for cleanup.
@Injectable()
export class ResizeObservableService implements OnDestroy {
  private resizeObserver: ResizeObserver;
  private notifiers: NextObserver<ResizeObserverEntry[]>[] = [];

  constructor() {
    this.resizeObserver = new ResizeObserver((entries: ResizeObserverEntry[]) => {
      this.notifiers.forEach(obs => obs.next(entries));
    });
  }

  ngOnDestroy(): void {
    this.resizeObserver.disconnect()
  }

  resizeObservable(elem: Element): Observable<ResizeObserverEntry> {
    this.resizeObserver.observe(elem);
    const newObserverCandidate = new Observable<ResizeObserverEntry[]>(
      (subscriber: Subscriber<ResizeObserverEntry[]>) => {
        this.notifiers.push(subscriber);

        return () => {
          const idx = this.notifiers.findIndex(val => val === subscriber);
          this.notifiers.splice(idx, 1);
          this.resizeObserver.unobserve(elem);
        };
      }
    );

    return newObserverCandidate.pipe(
      debounceTime(150),
      map(entries => entries.find(entry => entry.target === elem)),
      filter(Boolean),
    );
  }

  widthResizeObservable(elem: Element): Observable<number> {
    return this.resizeObservable(elem).pipe(
      map(entry => entry.borderBoxSize[0].inlineSize),
      filter(Boolean)
    );
  }
}

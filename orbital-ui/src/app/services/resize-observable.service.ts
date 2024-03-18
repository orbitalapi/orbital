import { Injectable } from '@angular/core';
import { filter, map, NextObserver, Observable, Subscriber } from "rxjs";
import { debounceTime } from 'rxjs/operators';
import { VyneServicesModule } from './vyne-services.module';

@Injectable({
  providedIn: VyneServicesModule,
})
export class ResizeObservableService {
  private resizeObserver: ResizeObserver;
  private notifiers: NextObserver<ResizeObserverEntry[]>[] = [];

  constructor() {
    this.resizeObserver = new ResizeObserver((entries: ResizeObserverEntry[]) => {
      this.notifiers.forEach(obs => obs.next(entries));
    });
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

import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from '@angular/core';
import {interval} from 'rxjs';
import * as moment from 'moment';
import {Observable} from 'rxjs/internal/Observable';
import {map} from 'rxjs/operators';

@Component({
  selector: 'app-counter-timer',
  template: `
    <span>{{ duration$ | async }}</span>
  `,
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CounterTimerComponent implements OnInit {

  constructor() {
  }

  @Input()
  startDate: Date;

  duration$: Observable<String>

  ngOnInit() {
    this.duration$ = interval(500)
      .pipe(
        map(() => {
          const duration = this.getElapsedTime().duration;
          return duration;
        })
      )
  }

  get duration(): string {
    return this.getElapsedTime().duration;
  }

  getElapsedTime(): Timespan {
    const fromDate = this.startDate || new Date();
    return Timespan.since(fromDate);

  }
}

export class Timespan {
  static since(fromDate: Date): Timespan {
    const totalMilliseconds = Math.floor((new Date().getTime() - fromDate.getTime()));
    return this.ofMillis(totalMilliseconds);
  }

  constructor(public readonly hours: number, public readonly minutes: number,
              public readonly seconds: number,
              public readonly millis: number,
              public readonly largestUnit: TimeUnit) {
  }


  static ofMillis(millis: number): Timespan {
    if (millis === null) {
      return new Timespan(
        0, 0, 0, 0, 'N/A'
      );
    }
    const momentDuration = moment.duration(millis, 'ms');
    if (momentDuration.hours() > 0) {
      return new Timespan(
        momentDuration.hours(), momentDuration.minutes(), momentDuration.seconds(), momentDuration.milliseconds(), 'hours'
      );
    } else if (momentDuration.minutes() > 0) {
      return new Timespan(
        momentDuration.hours(), momentDuration.minutes(), momentDuration.seconds(), momentDuration.milliseconds(), 'minutes'
      );
    } else if (momentDuration.seconds() > 0) {
      return new Timespan(
        momentDuration.hours(), momentDuration.minutes(), momentDuration.seconds(), momentDuration.milliseconds(), 'seconds'
      );
    } else {
      return new Timespan(
        momentDuration.hours(), momentDuration.minutes(), momentDuration.seconds(), momentDuration.milliseconds(), 'millis'
      );
    }
  }

  get duration(): string {
    if (this.largestUnit === 'N/A') {
      return 'N/A';
    }
    let suffix: string;
    if (this.largestUnit === 'seconds') {
      suffix = 's';
    } else if (this.largestUnit === 'millis') {
      suffix = 'ms';
    } else {
      suffix = '';
    }

    function pad(num: number): string {
      return (num < 10) ? `0${num}` : `${num}`;
    }

    if (this.largestUnit === 'hours') {
      return `${this.hours}:${pad(this.minutes)}:${pad(this.seconds)}`;
    } else if (this.largestUnit === 'minutes') {
      return `${this.minutes}:${pad(this.seconds)}`;
    } else if (this.largestUnit === 'millis') {
      return `${this.millis}${suffix}`;
    } else {
      return `${this.seconds}${suffix}`;
    }
  }
}

interface TimeSpan {
  hours: number;
  minutes: number;
  seconds: number;
  largestUnit: TimeUnit;
}

type TimeUnit = 'hours' | 'minutes' | 'seconds' | 'millis' | 'N/A';

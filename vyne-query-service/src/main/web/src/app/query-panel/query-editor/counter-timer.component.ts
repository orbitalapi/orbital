import {ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from '@angular/core';
import {interval} from 'rxjs';
import * as moment from 'moment';

@Component({
  selector: 'app-counter-timer',
  template: `
    <span>{{ duration }}</span>
  `,
  encapsulation: ViewEncapsulation.None
})
export class CounterTimerComponent implements OnInit {

  constructor(private changeDetector: ChangeDetectorRef) {
  }

  @Input()
  startDate: Date;

  ngOnInit() {
    interval(1000).subscribe(() => {
      if (!this.changeDetector['destroyed']) {
        this.changeDetector.detectChanges();
      }
    });
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

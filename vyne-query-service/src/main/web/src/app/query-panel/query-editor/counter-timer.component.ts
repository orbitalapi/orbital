import {ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from '@angular/core';
import {interval} from 'rxjs';

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
    let hours = 0;
    let minutes = 0;
    let seconds = 0;
    let largestUnit: TimeUnit = 'seconds';
    let displayMillis = 0;

    let totalSeconds = Math.floor(millis / 1000);

    if (totalSeconds >= 3600) {
      hours = Math.floor(totalSeconds / 3600);
      totalSeconds -= 3600 * hours;
      largestUnit = 'hours';
    }

    if (totalSeconds >= 60) {
      minutes = Math.floor(totalSeconds / 60);
      totalSeconds -= 60 * minutes;
      largestUnit = 'minutes';
    }

    if (totalSeconds < 1) {
      largestUnit = 'millis';
      displayMillis = millis;
    }

    seconds = totalSeconds;
    return new Timespan(
      hours, minutes, seconds, displayMillis, largestUnit
    );
  }

  get duration(): string {
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

type TimeUnit = 'hours' | 'minutes' | 'seconds' | 'millis';

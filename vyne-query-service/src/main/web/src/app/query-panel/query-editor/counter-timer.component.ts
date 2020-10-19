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
    const timespan = this.getElapsedTime();
    const suffix = (timespan.largestUnit === 'seconds') ? 's' : '';

    function pad(num: number): string {
      return (num < 10) ? `0${num}` : `${num}`;
    }

    if (timespan.largestUnit === 'hours') {
      return `${timespan.hours}:${pad(timespan.minutes)}:${pad(timespan.seconds)}`;
    } else if (timespan.largestUnit === 'minutes') {
      return `${timespan.minutes}:${pad(timespan.seconds)}`;
    } else {
      return `${timespan.seconds}${suffix}`;
    }
  }

  getElapsedTime(): TimeSpan {
    const fromDate = this.startDate || new Date();
    let totalSeconds = Math.floor((new Date().getTime() - fromDate.getTime()) / 1000);

    let hours = 0;
    let minutes = 0;
    let seconds = 0;
    let largestUnit: TimeUnit = 'seconds';

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

    seconds = totalSeconds;

    return {
      hours: hours,
      minutes: minutes,
      seconds: seconds,
      largestUnit
    };
  }


}

interface TimeSpan {
  hours: number;
  minutes: number;
  seconds: number;
  largestUnit: TimeUnit;
}

type TimeUnit = 'hours' | 'minutes' | 'seconds';

import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { isNullOrUndefined } from 'util';
import { RunningQueryStatus } from '../../services/active-queries-notification-service';
import { Observable } from 'rxjs/internal/Observable';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-query-editor-bottom-bar',
  template: `
    <span class="error-message">{{ error }}</span>
    <button mat-flat-button color="accent"
            class="button-small "
            *ngIf="(currentState$ | async) !== 'Running' && (currentState$ | async) !== 'Cancelling'"
            (click)="runQuery()">
      <img src="assets/img/tabler/player-play.svg" class="filter-white">
      Run
    </button>
    <div class="running-timer" *ngIf="(currentState$ | async) === 'Running'">
      <span class="loader"></span>
      <span>Running...&nbsp;</span>
      <app-counter-timer *ngIf="queryStarted" [startDate]="queryStarted"></app-counter-timer>


      <div class="progress"
           *ngIf="queryStarted && percentComplete > 0 && runningQueryStatus.queryType !== 'STREAMING' && runningQueryStatus.estimatedProjectionCount !== 0">
        <mat-progress-bar mode="determinate" [value]="percentComplete"></mat-progress-bar>
        <span>{{ runningQueryStatus.completedProjections}} of {{ runningQueryStatus.estimatedProjectionCount}}
          records</span>
      </div>

      <div class="progress"
           *ngIf="queryStarted && percentComplete > 0  && runningQueryStatus.queryType === 'STREAMING'">
        <mat-progress-bar mode="indeterminate" [value]="percentComplete"></mat-progress-bar>
        <span>{{ runningQueryStatus.completedProjections}}</span>
      </div>

      <button mat-stroked-button *ngIf="(currentState$ | async) === 'Running'" color="accent"
              (click)="cancelQuery.emit()"
      >Cancel
      </button>
    </div>

    <div class="running-timer" *ngIf="(currentState$ | async) === 'Cancelling'">
      <span class="loader"></span>
      <span>Cancelling...</span>
    </div>
  `,
  styleUrls: ['./bottom-bar.component.scss']
})
export class BottomBarComponent {

  @Input()
  currentState$: Observable<QueryState>;

  @Input()
  error: string;

  @Output()
  executeQuery = new EventEmitter<void>();

  @Input()
  queryStarted: Date;


  @Output()
  cancelQuery = new EventEmitter();

  @Input()
  runningQueryStatus: RunningQueryStatus | null;

  get percentComplete(): number | null {
    if (!this.runningQueryStatus) {
      return null;
    }
    if (isNullOrUndefined(this.runningQueryStatus.estimatedProjectionCount)) {
      return null;
    }
    return (this.runningQueryStatus.completedProjections / this.runningQueryStatus.estimatedProjectionCount) * 100;
  }

  runQuery() {
    this.error = null;
    this.queryStarted = new Date();
    this.executeQuery.emit();
  }

}

export type QueryState = 'Editing' | 'Running' | 'Result' | 'Error' | 'Cancelling';

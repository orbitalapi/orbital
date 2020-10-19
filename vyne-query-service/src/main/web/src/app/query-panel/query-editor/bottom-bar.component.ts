import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';

@Component({
  selector: 'app-query-editor-bottom-bar',
  template: `
    <div class="footer-bar">
      <button mat-raised-button color="accent"
              *ngIf="currentState !== 'Running'"
              (click)="runQuery()">Run
      </button>
      <div class="running-timer" *ngIf="currentState === 'Running'">
        <img class="loading-spinner" src="assets/img/loading-spinner-yellow.svg">
        <span>Running...&nbsp;</span>
        <app-counter-timer *ngIf="queryStarted" [startDate]="queryStarted"></app-counter-timer>
      </div>
      <div class="error-message" *ngIf="currentState === 'Error'">
        <span>{{ error }}</span>
      </div>
    </div>
  `,
  styleUrls: ['./bottom-bar.component.scss']
})
export class BottomBarComponent {

  @Input()
  currentState: QueryState;

  @Input()
  error: string;

  @Output()
  executeQuery = new EventEmitter<void>();

  @Input()
  queryStarted: Date;

  runQuery() {
    this.currentState = 'Running';
    this.queryStarted = new Date();
    this.executeQuery.emit();
  }

}

export type QueryState = 'Editing' | 'Running' | 'Result' | 'Error';

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Policy} from './policies';
import {Schema, Type} from '../services/schema';

@Component({
  selector: 'app-policy-manager',
  styleUrls: ['./policy-manager.component.scss'],
  template: `
    <div class="content">
      <mat-progress-bar mode="indeterminate" color="accent" *ngIf="loading"></mat-progress-bar>
      <div><p>Policies let you define rules that control who can access data through Vyne</p></div>
      <div class="empty-state" *ngIf="!policy">
        <div>There's no policy defined against {{ targetType?.name?.name }} yet.&nbsp;
          <a href="javascript:void" (click)="createNewPolicy.emit()">Create a new policy</a> now to get started.
        </div>
        <div class="button-container">
          <button mat-stroked-button (click)="createNewPolicy.emit()">Add new</button>
        </div>
      </div>
      <div class="policy-editor-container">
        <app-policy-editor [policy]="policy" [policyType]="targetType" *ngIf="policy" (save)="doSave()"
                           (cancel)="cancel()"
                           [schema]="schema"></app-policy-editor>
      </div>
    </div>
  `
})
export class PolicyManagerComponent {

  @Input()
  schema: Schema;

  @Input()
  policy: Policy;

  @Input()
  loading = false;

  @Output()
  save: EventEmitter<Policy> = new EventEmitter<Policy>();

  @Output()
  createNewPolicy: EventEmitter<void> = new EventEmitter<void>();

  @Input()
  targetType: Type;

  @Output()
  reset: EventEmitter<void> = new EventEmitter<void>();

  doSave() {
    this.save.emit(this.policy);
  }

  cancel() {
    this.reset.emit();
  }
}

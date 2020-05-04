import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Policy, PolicyStatement, RuleSet} from './policies';
import {Schema, Type} from '../services/schema';

@Component({
  selector: 'app-statement-editor',
  template: `
    <div class="statement-container editable-container">
      <div class="edit-button-container row-cell" *ngIf="!readonly">
        <mat-icon *ngIf="!editing" (click)="editing = true">edit</mat-icon>
        <mat-icon *ngIf="editing" (click)="editing = false">check</mat-icon>
        <mat-icon *ngIf="canDelete" (click)="deleteMe()">delete</mat-icon>
      </div>
      <div class="display-editor-container row-cell">
        <app-statement-display (edit)="editing = true" [statement]="statement" *ngIf="!editing"
                               [readonly]="readonly"
                               [policy]="policy"></app-statement-display>
        <div class="statement-switch-container" [ngSwitch]="statement.condition.type" *ngIf="editing">
          <app-case-condition-editor (statementUpdated)="onStatementUpdated()" *ngSwitchCase="'case'"
                                     [statement]="statement" [policyType]="policyType"
                                     [schema]="schema">

          </app-case-condition-editor>
          <app-else-editor *ngSwitchCase="'else'" [statement]="statement" [ruleSet]="policy.ruleSets[0]"
                           [policyType]="policyType"
                           (statementUpdated)="onStatementUpdated()"></app-else-editor>
        </div>
      </div>
    </div>`,
  styleUrls: ['./statement-editor.component.scss']
})
export class StatementEditorComponent {

  get editing(): boolean {
    return this.statement.editing;
  }

  set editing(value: boolean) {
    this.statement.editing = value;
  }

  @Input()
  schema: Schema;

  @Input()
  readonly: boolean;

  @Input()
  statement: PolicyStatement;

  @Input()
  policy: Policy;

  @Input()
  ruleset: RuleSet;

  @Input()
  policyType: Type;

  @Output()
  statementUpdated: EventEmitter<void> = new EventEmitter();

  @Output()
  deleteStatement: EventEmitter<PolicyStatement> = new EventEmitter();

  get canDelete(): boolean {
    const isLast = this.ruleset.statements.indexOf(this.statement) === this.ruleset.statements.length - 1;
    return !isLast;
  }

  onStatementUpdated() {
    this.statementUpdated.emit();
  }

  deleteMe() {
    this.deleteStatement.emit(this.statement);
  }
}

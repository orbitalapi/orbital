import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Policy, PolicyStatement, RuleSet} from "./policies";

@Component({
  selector: 'app-statement-editor',
  template: `
    <div class="statement-container">
      <div class="button-container row-cell">
        <mat-icon *ngIf="!editing" (click)="editing = true">edit</mat-icon>
        <mat-icon *ngIf="editing" (click)="editing = false">check</mat-icon>
        <mat-icon *ngIf="canDelete" (click)="deleteMe()">delete</mat-icon>
      </div>
      <div class="display-editor-container row-cell">
        <app-statement-display (edit)="editing = true" [statement]="statement" *ngIf="!editing" [policy]="policy"></app-statement-display>
        <div class="statement-switch-container" [ngSwitch]="statement.condition.text" *ngIf="editing">
          <app-case-condition-editor (statementUpdated)="onStatementUpdated()" *ngSwitchCase="'case'"
                                     [statement]="statement" [policyType]="policy.targetType"></app-case-condition-editor>
          <app-else-editor *ngSwitchCase="'else'" [statement]="statement" [ruleSet]="policy.rules[0]"
                           (statementUpdated)="onStatementUpdated()"></app-else-editor>
        </div>
      </div>
    </div>`,
  styleUrls: ['./statement-editor.component.scss']
})
export class StatementEditorComponent implements OnInit {

  get editing(): boolean {
    return this.statement.editing
  }
  set editing(value:boolean) {
    this.statement.editing = value
  }


  @Input()
  statement: PolicyStatement;

  @Input()
  policy: Policy;

  @Input()
  ruleset: RuleSet;

  @Output()
  statementUpdated: EventEmitter<void> = new EventEmitter();

  @Output()
  deleteStatement: EventEmitter<PolicyStatement> = new EventEmitter();

  get canDelete(): boolean {
    const isLast = this.ruleset.statements.indexOf(this.statement) == this.ruleset.statements.length - 1;
    return !isLast;
  }

  ngOnInit() {
  }

  onStatementUpdated() {
    this.statementUpdated.emit()
  }

  deleteMe() {
    this.deleteStatement.emit(this.statement)
  }
}

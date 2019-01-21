import {Component, Input, OnInit} from '@angular/core';
import {CaseCondition, Instruction, Policy, PolicyStatement, RuleSet} from "./policies";

@Component({
  selector: 'app-policy-editor',
  styleUrls: ['./policy-editor.component.scss'],
  template: `
    <div class="component-container">
      <form>
        <mat-form-field appearance="outline" hintLabel="Letters, numbers and punctuation only.  No spaces permitted" class="title-form-field">
          <mat-label>Policy title</mat-label>
          <input matInput placeholder="Policy title" [(ngModel)]="policy.name" name="title">
        </mat-form-field>
      </form>
      <div class="ruleset-container" *ngFor="let ruleset of policy.rules">
        <div class="statement-list-container">
          <div class="statement-container" *ngFor="let statement of ruleset.statements">
            <app-statement-editor [statement]="statement" [policy]="policy" [ruleset]="ruleset" (statementUpdated)="onStatementUpdated()"
                                  (deleteStatement)="deleteStatement(ruleset,$event)"></app-statement-editor>
          </div>
          <button mat-stroked-button (click)="addCase(ruleset)">Add case</button>
        </div>
      </div>
    </div>`
})
export class PolicyEditorComponent implements OnInit {

  @Input()
  policy: Policy;

  constructor() {
  }

  ngOnInit() {
  }

  addCase(ruleset: RuleSet) {
    ruleset.appendStatement(new PolicyStatement(new CaseCondition(), Instruction.permit(), true))
  }

  onStatementUpdated() {
    console.log(this.policy.src())
  }

  deleteStatement(ruleset: RuleSet, $event: PolicyStatement) {
    ruleset.removeStatement($event)
  }
}

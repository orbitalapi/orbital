import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {CaseCondition, PermitInstruction, Policy, PolicyStatement, RuleSet} from './policies';
import {Schema, Type} from '../services/schema';

@Component({
  selector: 'app-policy-editor',
  styleUrls: ['./policy-editor.component.scss'],
  template: `
    <div class="component-container">
      <form *ngIf="editing">
        <mat-form-field appearance="outline" hintLabel="Letters, numbers and punctuation only.  No spaces permitted"
                        class="title-form-field">
          <mat-label>Policy title</mat-label>
          <input matInput placeholder="Policy title" [(ngModel)]="policy.name.name" name="title">
        </mat-form-field>
      </form>
      <div *ngIf="!editing" class="read-only-title editable-container">
        <h4>{{ policy.name.name }}</h4>
        <div class="edit-button-container">
          <mat-icon (click)="editing = true">edit</mat-icon>
        </div>
      </div>

      <div class="ruleset-container" *ngFor="let ruleset of policy.ruleSets">
        <div class="statement-list-container">
          <div class="statement-container" *ngFor="let statement of ruleset.statements">
            <app-statement-editor [statement]="statement" [policy]="policy" [ruleset]="ruleset"
                                  (statementUpdated)="onStatementUpdated()"
                                  [policyType]="policyType"
                                  [readonly]="!editing"
                                  [schema]="schema"
                                  (deleteStatement)="deleteStatement(ruleset,$event)"></app-statement-editor>
          </div>
          <button mat-stroked-button (click)="addCase(ruleset)" *ngIf="editing">Add case</button>
        </div>
      </div>
      <div class="button-container" *ngIf="editing">
        <button mat-raised-button color="primary" (click)="submit()">Submit</button>
        <button mat-raised-button (click)="doCancel()">Cancel</button>
      </div>
    </div>`
})
export class PolicyEditorComponent {

  editing = false;

  @Input()
  policy: Policy;

  @Input()
  policyType: Type;

  @Input()
  schema: Schema;

  @Output()
  save = new EventEmitter();

  @Output()
  cancel = new EventEmitter();

  addCase(ruleset: RuleSet) {
    ruleset.appendStatement(new PolicyStatement(CaseCondition.empty(), new PermitInstruction(), true));
  }

  onStatementUpdated() {
    console.log(this.policy.src());
  }

  deleteStatement(ruleset: RuleSet, $event: PolicyStatement) {
    ruleset.removeStatement($event);
  }

  doCancel() {
    this.editing = false;
    this.cancel.emit();
  }

  submit() {
    this.save.emit();
  }
}

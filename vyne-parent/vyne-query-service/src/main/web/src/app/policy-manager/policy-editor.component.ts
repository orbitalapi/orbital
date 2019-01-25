import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {CaseCondition, Instruction, Policy, PolicyStatement, RuleSet} from "./policies";
import {TypesService} from "../services/types.service";

@Component({
  selector: 'app-policy-editor',
  styleUrls: ['./policy-editor.component.scss'],
  template: `
    <div class="component-container">
      <form>
        <mat-form-field appearance="outline" hintLabel="Letters, numbers and punctuation only.  No spaces permitted"
                        class="title-form-field">
          <mat-label>Policy title</mat-label>
          <input matInput placeholder="Policy title" [(ngModel)]="policy.name.name" name="title">
        </mat-form-field>
      </form>
      <div class="ruleset-container" *ngFor="let ruleset of policy.ruleSets">
        <div class="statement-list-container">
          <div class="statement-container" *ngFor="let statement of ruleset.statements">
            <app-statement-editor [statement]="statement" [policy]="policy" [ruleset]="ruleset"
                                  (statementUpdated)="onStatementUpdated()"
                                  (deleteStatement)="deleteStatement(ruleset,$event)"></app-statement-editor>
          </div>
          <button mat-stroked-button (click)="addCase(ruleset)">Add case</button>
        </div>
      </div>
      <div class="button-container">
        <button mat-raised-button color="primary" (click)="submit()">Submit</button>
        <button mat-raised-button (click)="doCancel()">Cancel</button>
      </div>
    </div>`
})
export class PolicyEditorComponent implements OnInit {

  @Input()
  policy: Policy;

  @Output()
  save = new EventEmitter();

  @Output()
  cancel = new EventEmitter();

  constructor(private schemaService: TypesService) {
  }

  ngOnInit() {
  }

  addCase(ruleset: RuleSet) {
    ruleset.appendStatement(new PolicyStatement(CaseCondition.empty(), Instruction.permit(), true))
  }

  onStatementUpdated() {
    console.log(this.policy.src())
  }

  deleteStatement(ruleset: RuleSet, $event: PolicyStatement) {
    ruleset.removeStatement($event)
  }

  doCancel() {
    this.cancel.emit()
  }

  submit() {
    this.save.emit()
  }
}

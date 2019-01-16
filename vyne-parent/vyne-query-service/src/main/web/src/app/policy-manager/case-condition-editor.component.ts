import {Component, EventEmitter, Input, Output} from '@angular/core';
import {
  CaseCondition,
  LiteralSubject,
  Operator,
  PolicyStatement,
  RelativeSubject,
  RelativeSubjectSource, SubjectType
} from "./policies";
import {Schema, Type, TypesService} from "../services/types.service";
import {Observable} from "rxjs";
import {MatSelectChange} from "@angular/material";
import {log} from "util";
import {isType} from "@angular/core/src/type";

@Component({
  selector: 'app-case-condition-editor',
  styleUrls: ['./case-condition-editor.component.scss'],
  template: `
    <div class="statement-editor-wrapper">
      <div class="line-wrapper">
        <span class="line-prefix line-component">when caller's</span>
        <div class="statement-parts-wrapper">
          <app-type-autocomplete
            class="fact-type-input line-component"
            floatLabel="never"
            displayFullName="false"
            [schema]="schema | async" (typeSelected)="onCallerTypeSelected($event)"
            placeholder="Select type"></app-type-autocomplete>
          <div class="operator-wrapper line-component">
            <mat-form-field floatLabel="never">
              <mat-select placeholder="Operator" (selectionChange)="onOperatorChange($event)"
                          [(value)]="selectedOperator">
                <mat-option *ngFor="let operator of operators" [value]="operator">
                  {{operator.label}}
                </mat-option>
              </mat-select>
            </mat-form-field>
          </div>
          <div class="rh-value-container" [ngSwitch]="selectedOperator?.operator.symbol">
            <app-equals-editor [caseCondition]="condition" [type]="policyType" [schema]="schema | async"
                               *ngSwitchDefault (statementUpdated)="statementUpdated.emit('')"
                               [literalOrProperty]="selectedOperator?.literalOrProperty"></app-equals-editor>
            <app-multivalue-editor [caseCondition]="condition" *ngSwitchCase="'in'"
                                   (statementUpdated)="statementUpdated.emit('')"></app-multivalue-editor>
          </div>
        </div>
      </div>
      <div class="line-wrapper">
        <span>then&nbsp;</span>
        <div class="statement-parts-wrapper">
          <app-instruction-selector [instruction]="statement.instruction"
                                    (statementUpdated)="statementUpdated.emit()"></app-instruction-selector>
        </div>
      </div>
    </div>`
})
export class CaseConditionEditorComponent {

  @Input()
  policyType: Type;

  @Output()
  statementUpdated: EventEmitter<string> = new EventEmitter();

  @Input()
  statement: PolicyStatement;

  get condition(): CaseCondition {
    return this.statement ? <CaseCondition>this.statement.condition : null;
  }

  private isType(operator:Operator,subjectType:SubjectType) {
    return function (caseCondition: CaseCondition) {
      if (!caseCondition || !caseCondition.rhSubject) return false;
      return caseCondition.rhSubject.type == subjectType && caseCondition.operator == operator;
    }
  }


  operators: DisplayOperator[] = [
    {operator: Operator.EQUALS, label: 'equals property', literalOrProperty: 'property', matches: this.isType(Operator.EQUALS,'RelativeSubject')},
    {operator: Operator.EQUALS, label: 'equals value', literalOrProperty: 'literal', matches: this.isType(Operator.EQUALS, 'LiteralSubject')},
    {operator: Operator.NOT_EQUAL, label: 'does not equal property', literalOrProperty: 'property', matches: this.isType(Operator.NOT_EQUAL, 'RelativeSubject')},
    {operator: Operator.NOT_EQUAL, label: 'does not equal value', literalOrProperty: 'literal', matches: this.isType(Operator.NOT_EQUAL, 'LiteralSubject')},
    {operator: Operator.IN, label: 'is in', matches: this.isType(Operator.IN, 'LiteralArraySubject')}
  ];

  schema: Observable<Schema>;

  get selectedOperator(): DisplayOperator {
    if (this.condition) {
      return this.operators.find(o => o.matches(this.condition))
    } else {
      return this.operators[0];
    }
  }

  set selectedOperator(value:DisplayOperator) {
    this.condition.operator = value.operator;
    console.log("selectedOperator changed to " + value.literalOrProperty);
  }

  constructor(private typeService: TypesService) {
    this.schema = typeService.getTypes()
  }

  onCallerTypeSelected(event: Type) {
    this.condition.lhSubject = new RelativeSubject(RelativeSubjectSource.CALLER, event);
    this.statementUpdated.emit("");
  }

  onOperatorChange(event: MatSelectChange) {
    const displayOperator = <DisplayOperator>event.value;
    this.condition.operator = event.value.operator;
    // if (displayOperator.literalOrProperty == 'literal') {
    //   this.condition.rhSubject = new LiteralSubject(null);
    // } else {
    //   this.condition.rhSubject = new RelativeSubject(null, null, null)
    // }
    this.statementUpdated.emit("");
  }
}

interface DisplayOperator {
  operator: Operator;
  label: string;
  literalOrProperty?: 'literal' | 'property';
  matches: (CaseCondition) => boolean
}

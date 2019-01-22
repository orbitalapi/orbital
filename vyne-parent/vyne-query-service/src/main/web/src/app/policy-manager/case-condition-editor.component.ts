import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CaseCondition, Operator, PolicyStatement, RelativeSubject, RelativeSubjectSource} from "./policies";
import {QualifiedName, Schema, Type} from "../services/schema";
import {TypesService} from "../services/types.service";
import {Observable} from "rxjs";
import {MatSelectChange} from "@angular/material";
import {map} from "rxjs/operators";

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
            [selectedTypeName]="selectedCallerType"
            [schema]="schema | async" (typeSelected)="onCallerTypeSelected($event)"
            placeholder="Select type"></app-type-autocomplete>
          <div class="operator-wrapper line-component">
            <mat-form-field floatLabel="never">
              <mat-select placeholder="Operator" (selectionChange)="onOperatorChange($event)"
                          [value]="condition?.displayOperator">
                <mat-option *ngFor="let operator of operators" [value]="operator">
                  {{operator.label}}
                </mat-option>
              </mat-select>
            </mat-form-field>
          </div>
          <div class="rh-value-container" [ngSwitch]="condition?.displayOperator?.operator.symbol">
            <app-equals-editor [caseCondition]="condition" [type]="policyType | async" [schema]="schema | async"
                               *ngSwitchDefault (statementUpdated)="statementUpdated.emit('')"
                               [literalOrProperty]="condition?.displayOperator?.literalOrProperty"></app-equals-editor>
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
  policyTypeName: QualifiedName;

  @Output()
  statementUpdated: EventEmitter<string> = new EventEmitter();

  @Input()
  statement: PolicyStatement;

  get policyType(): Observable<Type> {
    // if (!this.policyTypeName || !this.schema) return null;
    return this.schema.pipe(map((s:Schema) => {
      return s.types.find(t => t.name.fullyQualifiedName == this.policyTypeName.fullyQualifiedName)
    }))
  }

  get condition(): CaseCondition {
    return this.statement ? <CaseCondition>this.statement.condition : null;
  }


  operators = Operator.displayOperators;

  schema: Observable<Schema>;

  constructor(private typeService: TypesService) {
    this.schema = typeService.getTypes()
  }

  get selectedCallerType(): QualifiedName {
    if (!this.condition || !this.condition.lhSubject) return null;
    if (this.condition.lhSubject.type !== 'RelativeSubject') return null;
    const relativeSubject = <RelativeSubject>this.condition.lhSubject;
    return relativeSubject.targetTypeName
  }

  onCallerTypeSelected(type: Type) {
    if (!type) return;
    this.condition.lhSubject = new RelativeSubject(RelativeSubjectSource.CALLER, type.name);
    this.statementUpdated.emit("");
    console.log("type selected")
  }

  onOperatorChange(event: MatSelectChange) {
    this.condition.displayOperator = event.value;
    // if (displayOperator.literalOrProperty == 'literal') {
    //   this.condition.rhSubject = new LiteralSubject(null);
    // } else {
    //   this.condition.rhSubject = new RelativeSubject(null, null, null)
    // }
    this.statementUpdated.emit("");
  }
}


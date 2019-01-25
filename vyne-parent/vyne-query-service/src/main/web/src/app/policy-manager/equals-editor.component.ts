import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {QualifiedName, Schema, Type, TypeReference} from "../services/schema";
import {CaseCondition, LiteralSubject, RelativeSubject, RelativeSubjectSource} from "./policies";

@Component({
  selector: 'app-equals-editor',
  styleUrls: ['./equals-editor.component.scss'],
  template: `
    <div class="component-container" [ngSwitch]="literalOrProperty">
      <div class="literal-input-container" *ngSwitchCase="'literal'">
        <mat-form-field floatLabel="never">
          <input matInput placeholder="Value" [value]="literalSubject?.value" (change)="onLiteralValueUpdated($event)">
        </mat-form-field>
      </div>
      <div class="property-input-container" *ngSwitchCase="'property'">
         <app-type-autocomplete
          class="fact-type-input line-component"
          floatLabel="never"
          displayFullName="false"
          [selectedTypeName]="selectedProperty"
          [schema]="schema" (typeSelected)="onCallerTypeSelected($event)"
          placeholder="Select type"></app-type-autocomplete>
        <span class="property-explainer">related to this {{type?.name.name}}</span>
      </div>
    </div>
  `,
})
export class EqualsEditorComponent implements OnInit {
  private _caseCondition: CaseCondition;
  @Input()
  get caseCondition(): CaseCondition {
    return this._caseCondition;
  }

  set caseCondition(value: CaseCondition) {
    this._caseCondition = value;
  }

  @Input()
  literalOrProperty: 'literal' | 'property';

  @Input()
  type: Type;


  @Input()
  schema: Schema;

  get properties(): QualifiedName[] {
    if (!this.type) return [];
    return Object.values(this.type.attributes).map((typeRef: TypeReference) => typeRef.name)
  }

  @Output()
  statementUpdated: EventEmitter<string> = new EventEmitter();

  get literalSubject(): LiteralSubject {
    if (!this.caseCondition || !this.caseCondition.rhSubject || this.caseCondition.rhSubject.type != "LiteralSubject") return null;
    return <LiteralSubject>this.caseCondition.rhSubject
  }

  // literalOrProperty: boolean = this.LITERAL;

  ngOnInit() {
  }

  get selectedProperty(): QualifiedName {
    if (!this.caseCondition || !this.caseCondition.rhSubject || this.caseCondition.rhSubject.type !== "RelativeSubject") {
      return null
    }
    const relativeSubject = this.caseCondition.rhSubject as RelativeSubject;
    // to match the value in the drop-down, return the property from the type
    return relativeSubject.targetTypeName
  }

  set selectedProperty(value: QualifiedName) {
    this.caseCondition.rhSubject = new RelativeSubject(RelativeSubjectSource.THIS, value, null);
    this.statementUpdated.emit("")
  }

  onLiteralValueUpdated($event) {
    const value = $event.target.value;
    this._caseCondition.rhSubject = new LiteralSubject(value);
    this.statementUpdated.emit("")
  }

  onCallerTypeSelected(type: Type) {
    if (type) {
      this.selectedProperty = type.name
    }

  }
}

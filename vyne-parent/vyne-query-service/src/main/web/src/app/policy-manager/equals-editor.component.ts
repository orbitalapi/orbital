import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {QualifiedName, Schema, Type, TypeReference} from "../services/schema";
import {FormControl} from "@angular/forms";
import {Observable} from "rxjs";
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
        <mat-form-field style="width: 100%" floatLabel="never">
          <mat-select placeholder="Property" [(value)]="selectedProperty">
            <mat-option *ngFor="let property of properties" [value]="property">
              {{ property.name }}
            </mat-option>
          </mat-select>
        </mat-form-field>
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
    if (value && value.rhSubject && value.rhSubject.type === "RelativeSubject") {
      const relativeSubject: RelativeSubject = <RelativeSubject>value.rhSubject;
      this.propertyNameInput.setValue(relativeSubject.propertyName);
    }
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

  propertyNameInput = new FormControl();

  filteredPropertyNames: Observable<string[]>;

  get literalSubject(): LiteralSubject {
    if (!this.caseCondition || !this.caseCondition.rhSubject || this.caseCondition.rhSubject.type != "LiteralSubject") return null;
    return <LiteralSubject>this.caseCondition.rhSubject
  }

  // literalOrProperty: boolean = this.LITERAL;

  ngOnInit() {
  }

  get selectedProperty(): QualifiedName {
    if (!this.caseCondition || this.caseCondition.rhSubject.type !== "RelativeSubject") {
      return null
    }
    const relativeSubject = this.caseCondition.rhSubject as RelativeSubject;
    // to match the value in the drop-down, return the property from the type
    return this.properties.find(p => p.fullyQualifiedName == relativeSubject.targetTypeName.fullyQualifiedName)
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
}

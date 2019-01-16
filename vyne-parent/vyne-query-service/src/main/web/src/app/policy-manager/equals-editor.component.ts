import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema, Type} from "../services/types.service";
import {FormControl} from "@angular/forms";
import {map, startWith} from "rxjs/operators";
import {Observable} from "rxjs";
import {MatAutocompleteSelectedEvent} from "@angular/material";
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
          <input type="text" placeholder="Property name" matInput
                 [matAutocomplete]="auto"
                 [formControl]="propertyNameInput"
                 required>
          <mat-autocomplete #auto="matAutocomplete" autoActiveFirstOption (select)="onPropertySelected($event)"
                            (optionSelected)="onPropertySelected($event)">
            <mat-option *ngFor="let propertyName of filteredPropertyNames | async" [value]="propertyName">
              {{ propertyName }}
            </mat-option>
          </mat-autocomplete>
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
    this.filteredPropertyNames = this.propertyNameInput.valueChanges.pipe(
      startWith(''),
      map(value => this._filter(value))
    );
  }

  onPropertySelected(event: MatAutocompleteSelectedEvent) {
    const selectedPropertyName = event.option.value;
    const selectedTypeRef = this.type.attributes[selectedPropertyName];
    const selectedType = this.schema.types.find((t) => t.name.fullyQualifiedName == selectedTypeRef.fullyQualifiedName)
    this.caseCondition.rhSubject = new RelativeSubject(RelativeSubjectSource.THIS, selectedType, selectedPropertyName);
    this.statementUpdated.emit("")
  }

  private _filter(value: string): string[] {
    if (!this.type) return [];
    const filterValue = value.toLowerCase();
    const attributeNames = Object.keys(this.type.attributes);
    if (!value) return attributeNames;

    return attributeNames.filter(option => option.toLowerCase().indexOf(filterValue) !== -1);
  }

  onLiteralValueUpdated($event) {
    const value = $event.target.value;
    this._caseCondition.rhSubject = new LiteralSubject(value);
    this.statementUpdated.emit("")
  }
}

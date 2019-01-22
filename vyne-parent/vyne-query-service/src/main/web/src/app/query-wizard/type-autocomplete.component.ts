import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {QualifiedName, Schema, Type} from "../services/types.service";
import {FormControl} from "@angular/forms";
import {map, startWith} from "rxjs/operators";
import {Observable} from "rxjs";
import {FloatLabelType, MatAutocompleteSelectedEvent} from "@angular/material";

@Component({
  selector: 'app-type-autocomplete',
  template: `
    <mat-form-field style="width: 100%" [floatLabel]="floatLabel">
      <input type="text" [placeholder]="placeholder" matInput
             [matAutocomplete]="auto"
             [formControl]="filterInput"
             required>
      <mat-autocomplete #auto="matAutocomplete" autoActiveFirstOption (select)="onTypeSelected($event)"
                        (optionSelected)="onTypeSelected($event)">
        <mat-option *ngFor="let type of filteredTypes | async" [value]="type.name.fullyQualifiedName">
          {{type.name.name}} ({{type.name.fullyQualifiedName}})
        </mat-option>
      </mat-autocomplete>
    </mat-form-field>`
})
export class TypeAutocompleteComponent implements OnInit {

  @Input()
  placeholder: string;
  @Input()
  schema: Schema;
  @Input()
  floatLabel: FloatLabelType = 'auto';

  filteredTypes: Observable<Type[]>;

  filterInput = new FormControl();

  private _selectedType: Type;

  @Input()
  set selectedTypeName(name: QualifiedName) {
    // TODO : Could this cause issues because the schema isn't provided yet?
    const type = this.schema.types.find(t => t.name.fullyQualifiedName == name.fullyQualifiedName)
    this.selectedType = type
  }

  @Input()
  set selectedType(value: Type) {
    this.setSelectedTypeName(value);
    this.typeSelected.emit(value);
    this.selectedTypeChange.emit(value);
    this.setSelectedTypeName(value);
    this._selectedType = value;
  }

  get selectedType(): Type {
    return this._selectedType;
  }

  @Output()
  selectedTypeChange = new EventEmitter<Type>();

  // Deprecated - bind to selectedType / selectedTypeChange event
  @Output()
  typeSelected = new EventEmitter<Type>();

  @Input()
  displayFullName: boolean = true;

  ngOnInit() {
    this.filteredTypes = this.filterInput.valueChanges.pipe(
      startWith(''),
      map(value => this._filter(value))
    );
  }

  onTypeSelected(event: MatAutocompleteSelectedEvent) {
    console.log("onTypeSelected");
    const eventType = this.schema.types.find(type => type.name.fullyQualifiedName == event.option.value);
    this.selectedType = eventType
  }

  private setSelectedTypeName(selectedType: Type) {
    if (!selectedType) {
      this.filterInput.setValue(null)
      // this.selectedTypeDisplayName = null;
    } else {
      const selectedTypeDisplayName = (this.displayFullName) ? selectedType.name.fullyQualifiedName : selectedType.name.name;
      this.filterInput.setValue(selectedTypeDisplayName);
    }
  }

  private _filter(value: string): Type[] {
    if (!this.schema) return [];
    const filterValue = value.toLowerCase();
    return this.schema.types.filter(option => option.name.fullyQualifiedName.toLowerCase().indexOf(filterValue) !== -1);
  }


}

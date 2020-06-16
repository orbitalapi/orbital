import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {QualifiedName, Schema, Type} from '../services/schema';
import {FormControl} from '@angular/forms';
import {map, startWith} from 'rxjs/operators';
import {Observable} from 'rxjs';
import {FloatLabelType} from '@angular/material/esm2015';
import {MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import {COMMA, ENTER} from '@angular/cdk/keycodes';

@Component({
  selector: 'app-type-autocomplete',
  template: `
    <mat-form-field style="width: 100%" [floatLabel]="floatLabel">
      <mat-chip-list #chipList *ngIf="multiSelect">
        <mat-chip
          *ngFor="let selectedType of selectedTypes"
          selectable="true"
          removable="true"
          (removed)="remove(selectedType)">
          {{selectedType.name.name}}
          <mat-icon matChipRemove>cancel</mat-icon>
        </mat-chip>
        <input
          [placeholder]="placeholder"
          #chipInput
          [formControl]="filterInput"
          [matAutocomplete]="auto"
          [matChipInputFor]="chipList"
          [matChipInputSeparatorKeyCodes]="separatorKeysCodes"
          matChipInputAddOnBlur="true"
          (matChipInputTokenEnd)="add($event)">
      </mat-chip-list>
      <input type="text"
             *ngIf="!multiSelect"
             [placeholder]="placeholder" matInput
             [matAutocomplete]="auto"
             [formControl]="filterInput"
             required>
      <mat-autocomplete #auto="matAutocomplete" autoActiveFirstOption (select)="onTypeSelected($event)"
                        (optionSelected)="onTypeSelected($event)">
        <mat-option *ngFor="let type of filteredTypes | async" [value]="type.name.fullyQualifiedName">
          {{type.name.name}} ({{type.name.fullyQualifiedName}})
        </mat-option>
      </mat-autocomplete>
      <mat-hint *ngIf="hint" align="start">{{ hint }}</mat-hint>
    </mat-form-field>`
})
export class TypeAutocompleteComponent implements OnInit {
  separatorKeysCodes: number[] = [ENTER, COMMA];

  @ViewChild('chipInput' , {static: false}) chipInput: ElementRef<HTMLInputElement>;

  @Input()
  multiSelect = false;

  @Input()
  placeholder: string;
  @Input()
  schema: Schema;
  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  selectedTypes: Type[] = [];

  @Input()
  hint: string;

  @Output()
  selectedTypesChange = new EventEmitter<Type[]>();

  @Input()
  get selectedTypeNames(): string[] {
    return this.selectedTypes.map(t => t.name.fullyQualifiedName);
  }

  set selectedTypeNames(value: string[]) {
    this.selectedTypes = value.map(name => this.getTypeByName(QualifiedName.from(name)));
  }

  filteredTypes: Observable<Type[]>;

  filterInput = new FormControl();

  private _selectedType: Type;

  @Input()
  set selectedTypeName(name: QualifiedName) {
    if (!name) {
      this.selectedType = null;
    } else {
      // TODO : Could this cause issues because the schema isn't provided yet?
      this.selectedType = this.getTypeByName(name);

    }
  }

  private getTypeByName(name: QualifiedName) {
    return this.schema.types.find(t => t.name.fullyQualifiedName === name.fullyQualifiedName);
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
  displayFullName = true;

  ngOnInit() {
    this.filteredTypes = this.filterInput.valueChanges.pipe(
      startWith(''),
      map(value => this._filter(value))
    );
  }

  onTypeSelected(event: MatAutocompleteSelectedEvent) {
    console.log('onTypeSelected');
    const eventType = this.getTypeByName(QualifiedName.from(event.option.value));
    if (this.multiSelect) {
      this.selectedTypes.push(eventType);
      if (this.chipInput) {
        this.chipInput.nativeElement.value = '';
      }
      this.filterInput.setValue('');
      this.selectedTypesChange.emit(this.selectedTypes);
    } else {
      this.selectedType = eventType;
    }

  }

  private setSelectedTypeName(selectedType: Type) {
    if (!selectedType) {
      this.filterInput.setValue(null);
      // this.selectedTypeDisplayName = null;
    } else {
      const selectedTypeDisplayName = (this.displayFullName) ? selectedType.name.fullyQualifiedName : selectedType.name.name;
      this.filterInput.setValue(selectedTypeDisplayName);
    }
  }

  private _filter(value: string): Type[] {
    if (!this.schema || !value) { return []; }
    const filterValue = value.toLowerCase();
    return this.schema.types.filter(option => option.name.fullyQualifiedName.toLowerCase().indexOf(filterValue) !== -1);
  }


  remove(type: Type) {
    this.selectedTypes.splice(this.selectedTypes.indexOf(type, 1));
    this.selectedTypesChange.emit(this.selectedTypes);
  }

  add($event) {
    // console.log($event)
  }
}

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema, Type} from "../services/types.service";
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
             [value]="selectedTypeDisplayName"
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

  @Output()
  typeSelected = new EventEmitter<Type>();

  @Input()
  displayFullName: boolean = true;

  selectedTypeDisplayName: string;

  ngOnInit() {
    this.filteredTypes = this.filterInput.valueChanges.pipe(
      startWith(''),
      map(value => this._filter(value))
    );
  }

  onTypeSelected(event: MatAutocompleteSelectedEvent) {
    const selectedType = this.schema.types.find(type => type.name.fullyQualifiedName == event.option.value);
    this.typeSelected.emit(selectedType);
    this.selectedTypeDisplayName = (this.displayFullName) ? selectedType.name.fullyQualifiedName : selectedType.name.name;
  }

  private _filter(value: string): Type[] {
    if (!this.schema) return [];
    const filterValue = value.toLowerCase();
    return this.schema.types.filter(option => option.name.fullyQualifiedName.toLowerCase().indexOf(filterValue) !== -1);
  }


}

import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Operation, QualifiedName, Schema, SchemaMember, SchemaMemberType, Service, Type} from '../services/schema';
import {FormControl} from '@angular/forms';
import {map, startWith} from 'rxjs/operators';
import {Observable} from 'rxjs';
import {FloatLabelType, MatAutocompleteSelectedEvent} from '@angular/material';
import {COMMA, ENTER} from '@angular/cdk/keycodes';
import {MatFormFieldAppearance} from '@angular/material/form-field';

/**
 * More flexible version of type auto complete, but does not allow multi-select (for simplicity ... can add in the future).
 */
@Component({
  selector: 'app-schema-member-autocomplete',
  styleUrls: ['./type-autocomplete.component.scss'],
  template: `
    <mat-form-field style="width: 100%" [floatLabel]="floatLabel" [appearance]="appearance">
      <mat-label *ngIf="label">{{ label }}</mat-label>
      <input type="text"
             [placeholder]="placeholder" matInput
             [matAutocomplete]="auto"
             [formControl]="filterInput"
      >
      <mat-autocomplete #auto="matAutocomplete" autoActiveFirstOption
                        (optionSelected)="onMemberSelected($event)">
        <mat-option *ngFor="let member of filteredMembers | async" [value]="member.name.fullyQualifiedName">
          <span class="typeName">{{member.name.name}}</span>
          <span class="inline mono-badge">{{member.name.fullyQualifiedName}}</span>
          <span class="documentation">{{member.typeDoc}}</span>
        </mat-option>
      </mat-autocomplete>
      <mat-hint *ngIf="hint" align="start">{{ hint }}</mat-hint>
    </mat-form-field>`
})
export class SchemaMemberAutocompleteComponent implements OnInit {
  separatorKeysCodes: number[] = [ENTER, COMMA];

  @ViewChild('chipInput', {static: false}) chipInput: ElementRef<HTMLInputElement>;

  @Input()
  appearance: MatFormFieldAppearance = 'standard';

  @Input()
  placeholder: string;

  @Input()
  schemaMemberType: SchemaMemberType = 'TYPE';

  @Input()
  schema: Schema;

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  hint: string;

  @Output()
  selectedMemberChange = new EventEmitter<SchemaMember>();

  @Input()
  displayFullName = true;

  @Input()
  label: string;

  filteredMembers: Observable<SchemaMember[]>;

  filterInput = new FormControl();

  private _selectedMember: SchemaMember;

  @Input()
  set selectedMemberName(name: QualifiedName) {
    if (!name) {
      this.selectedMember = null;
    } else {
      // TODO : Could this cause issues because the schema isn't provided yet?
      this.selectedMember = this.getMemberByName(name);
    }
  }

  private getMemberByName(name: QualifiedName): SchemaMember {
    return this.schema.members.find(t => t.name.fullyQualifiedName === name.fullyQualifiedName);
  }

  @Input()
  set selectedMember(value: SchemaMember) {
    this.setSelectedMemberName(value);
    this.selectedMemberChange.emit(value);
    this.setSelectedMemberName(value);
    this._selectedMember = value;
  }

  get selectedMember(): SchemaMember {
    return this._selectedMember;
  }

  ngOnInit() {
    this.filteredMembers = this.filterInput.valueChanges.pipe(
      startWith(''),
      map(value => this._filter(value))
    );
  }

  onMemberSelected(event: MatAutocompleteSelectedEvent) {
    const eventType = this.getMemberByName(QualifiedName.from(event.option.value));
    this.selectedMember = eventType;
  }

  private setSelectedMemberName(selectedMember: SchemaMember) {
    if (!selectedMember) {
      this.filterInput.setValue(null);
      // this.selectedTypeDisplayName = null;
    } else {
      const selectedTypeDisplayName = (this.displayFullName) ? selectedMember.name.fullyQualifiedName : selectedMember.name.name;
      this.filterInput.setValue(selectedTypeDisplayName);
    }
  }

  private _filter(value: string): SchemaMember[] {
    if (!this.schema || !value) {
      return [];
    }
    const filterValue = value.toLowerCase();
    return this.schema.members.filter(option => {
      return option.kind === this.schemaMemberType &&
        option.name.fullyQualifiedName.toLowerCase().indexOf(filterValue) !== -1;
    });
  }
}

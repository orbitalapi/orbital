import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {Operation, QualifiedName, Schema, SchemaMember, SchemaMemberType, Service, Type} from '../services/schema';
import {FormControl} from '@angular/forms';
import {map, startWith} from 'rxjs/operators';
import {Observable} from 'rxjs';
import {COMMA, ENTER} from '@angular/cdk/keycodes';
import {FloatLabelType, MatFormFieldAppearance} from '@angular/material/form-field';
import {MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import {ConnectorSummary, ConnectorType} from "../db-connection-editor/db-importer.service";

/**
 * More flexible version of type auto complete, but does not allow multi-select (for simplicity ... can add in the future).
 */
@Component({
  selector: 'app-connection-name-autocomplete',
  styleUrls: ['./type-autocomplete.component.scss'],
  template: `
    <mat-form-field style="width: 100%" [floatLabel]="floatLabel" [appearance]="appearance">
      <mat-label *ngIf="label">{{ label }}</mat-label>
      <input type="text"
             [placeholder]="placeholder" matInput
             [matAutocomplete]="auto"
             [formControl]="filterInput"
             [disabled]="!enabled"
      >
      <mat-autocomplete #auto="matAutocomplete" autoActiveFirstOption
                        (optionSelected)="onConnectionSelected($event)">
        <mat-option *ngFor="let connection of filteredConnections | async" [value]="connection.connectionName">
          <span class="typeName">{{connection.connectionName}}</span>
        </mat-option>
      </mat-autocomplete>
      <mat-hint *ngIf="hint" align="start">{{ hint }}</mat-hint>
    </mat-form-field>`
})
export class ConnectionNameAutocompleteComponent implements OnInit {

  separatorKeysCodes: number[] = [ENTER, COMMA];

  @ViewChild('chipInput') chipInput: ElementRef<HTMLInputElement>;

  @Input()
  appearance: MatFormFieldAppearance = 'standard';

  @Input()
  placeholder: string;

  @Input()
  schemaMemberType: SchemaMemberType = 'TYPE';

  @Input()
  connections: ConnectorSummary[];

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  hint: string;

  private _enabled = true;

  @Input()
  get enabled(): boolean {
    return this._enabled;
  }

  set enabled(value: boolean) {
    if (value === this._enabled) { return; }
    this._enabled = value;
    if (this.enabled) {
      this.filterInput.enable();
    } else {
      this.filterInput.disable();
    }
  }

  @Output()
  selectedConnectionChange = new EventEmitter<ConnectorSummary>();

  @Input()
  label: string;

  @Input()
  connectionType?: ConnectorType

  filteredConnections: Observable<ConnectorSummary[]>;

  filterInput = new FormControl();

  private _selectedConnection: ConnectorSummary;

  @Input()
  set selectConnectionName(name: String) {
    if (!name) {
      this.selectedConnection = null;
    } else {
      // TODO : Could this cause issues because the schema isn't provided yet?
      this.selectedConnection = this.getConnectionByName(name);
    }
  }

  private getConnectionByName(connectionName: String): ConnectorSummary {
    return this.connections.find(t => t.connectionName === connectionName);
  }

  @Input()
  set selectedConnection(value: ConnectorSummary) {
    this.setSelectedConnectionName(value);
    this.selectedConnectionChange.emit(value);
    this.setSelectedConnectionName(value);
    this._selectedConnection = value;
  }

  get selectedConnection(): ConnectorSummary {
    return this._selectedConnection;
  }

  ngOnInit() {
    this.filteredConnections = this.filterInput.valueChanges.pipe(
      startWith(''),
      map(value => this._filter(value))
    );
  }

  onConnectionSelected(event: MatAutocompleteSelectedEvent) {
    const eventType = this.getConnectionByName(event.option.value);
    this.selectedConnection = eventType;
  }

  private setSelectedConnectionName(selectedConnection: ConnectorSummary) {
    if (!selectedConnection) {
      this.filterInput.setValue(null);
      // this.selectedTypeDisplayName = null;
    } else {
      this.filterInput.setValue(selectedConnection.connectionName);
    }
  }

  private _filter(value: string): ConnectorSummary[] {
    if (!this.connections || !value) {
      return [];
    }
    const filterValue = value.toLowerCase();
    return this.connections.filter( connection => {
        const nameMatch = connection.connectionName.toLowerCase().indexOf(filterValue) !== -1;
        if (this.connectionType) {
          return nameMatch && connection.type === this.connectionType;
        } else
          return nameMatch;
      }
    )
  }
}

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Operation, Parameter, QualifiedName, Schema, SchemaMember} from '../../services/schema';
import {AbstractControl, FormControl, FormGroup, Validators} from '@angular/forms';
import {PipelineTransportSpec} from '../pipelines.service';
import {BaseTransportConfigEditor} from './base-transport-config-editor';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-polling-input-config',
  template: `
    <div [formGroup]="config">
      <app-form-row title="Operation" helpText="Select an operation published to Vyne to call periodically">
        <app-schema-member-autocomplete
          [enabled]="editable"
          appearance="outline"
          label="Operation"
          [schema]="schema"
          [selectedMemberName]="selectedOperationName"
          (selectedMemberChange)="onOperationSelected($event)"
          schemaMemberType="OPERATION"></app-schema-member-autocomplete>
      </app-form-row>
      <app-form-row title="Schedule"
                    helpText="Define the schedule to poll.  Use a cron job syntax, or select one of the examples provided to get started">
        <mat-form-field appearance="outline">
          <mat-label>Poll schedule</mat-label>
          <input matInput placeholder="Enter or select a cron schedule" aria-label="State" [matAutocomplete]="auto"
                 formControlName="pollSchedule"
          >
          <mat-autocomplete #auto="matAutocomplete">
            <mat-option class="schedule-selector" *ngFor="let pollSchedule of pollSchedules"
                        [value]="pollSchedule.value">
              <div class="header">{{ pollSchedule.label }}</div>
              <div class="description">{{ pollSchedule.value }}</div>
            </mat-option>
          </mat-autocomplete>
        </mat-form-field>
      </app-form-row>

      <app-form-row title="Parameters"
                    helpText="Define parameters to pass to the operation.  Special values are allowed"
                    *ngIf="selectedOperationParameterInputs">
        <div *ngFor="let parameter of selectedOperationParameterInputs | keyvalue" style="flex-grow: 1">
          <mat-form-field appearance="outline" style="width: 100%">
            <mat-label>{{ parameter.key }}</mat-label>
            <input matInput [placeholder]="parameter.key" [formControl]="parameter.value"
                   [matAutocomplete]="parametersAutoComplete">
            <mat-autocomplete #parametersAutoComplete="matAutocomplete">
              <mat-option class="schedule-selector" *ngFor="let pipelineParameter of pipelineParameters"
                          [value]="pipelineParameter.value">
                <div class="header">{{ pipelineParameter.label }}</div>
                <div class="description">{{ pipelineParameter.value }}</div>
              </mat-option>
            </mat-autocomplete>
          </mat-form-field>
        </div>
      </app-form-row>
    </div>
  `,
  styleUrls: ['./polling-input-config.component.scss']
})
export class PollingInputConfigComponent extends BaseTransportConfigEditor {

  config: FormGroup;

  @Output()
  configValueChanged = new EventEmitter<any>();

  selectedOperationName: QualifiedName;
  selectedOperation: Operation;
  selectedOperationParameterInputs: { [key: string]: AbstractControl };

  pollSchedules = [
    {label: 'Every 10 seconds', value: '*/10 * * * * *'},
    {label: 'Every minute', value: '* * * * * *'},
    {label: 'Every hour', value: '0 0 * * * *'},
    {label: 'Midnight every day', value: '0 0 0 * * *'},
    {label: '10:15am every day', value: '0 0 15 10 * ?'},
    {label: '10:15am every week day', value: '0 0 15 10 * MON-FRI'}
  ];

  pipelineParameters = [
    {label: 'The last time this pipeline poll completed', value: '\$pipeline.lastRunTime'},
    {label: 'The current time', value: '\$env.now'}
  ];

  constructor() {
    super();
    this.config = new FormGroup({
        operationName: new FormControl('', Validators.required),
        pollSchedule: new FormControl('', Validators.required),
        parameterMap: new FormGroup({})
      }
    );
    this.config.valueChanges.subscribe(e => this.configValueChanged.emit(e));
  }

  updateFormValues(value: PipelineTransportSpec, schema: Schema) {

    // Set up the operationName before patching the value, as this configures
    // the FormControls for the operation params
    if (value.operationName) {
      this.selectedOperationName = QualifiedName.from(value.operationName);
      const {operation, name, params} = getOperationFromQualifiedName(this.selectedOperationName, schema);
      this.handleSelectedOperationUpdated(name, operation, params);
    }

    this.config.patchValue(value);

  }


  afterEnabledUpdated(value: boolean) {
    value ? this.config.enable() : this.config.disable();
  }

  onOperationSelected($event: SchemaMember) {
    const {operation, name, params} = getOperationFromMember($event, this.schema);
    if (this.selectedOperationName && this.selectedOperationName.fullyQualifiedName === name.fullyQualifiedName) {
      // Nothing has changed, so bail
      return;
    }
    this.handleSelectedOperationUpdated(name, operation, params);
  }


  private handleSelectedOperationUpdated(name: QualifiedName, operation: Operation, params: Parameter[]) {
    const fullyQualifiedName: string = name ? name.fullyQualifiedName : null;

    this.selectedOperation = operation;
    this.selectedOperationName = name;
    this.config.get('operationName').setValue(fullyQualifiedName);
    const selectedOperationParameterInputs: { [key: string]: AbstractControl } = {};
    params.forEach(p => {
      const controlName = p.name || p.type.shortDisplayName;
      selectedOperationParameterInputs[controlName] = new FormControl('');
    });
    const parametersFormGroup = new FormGroup(selectedOperationParameterInputs);
    this.config.setControl('parameterMap', parametersFormGroup);
    if (!this.editable) {
      parametersFormGroup.disable();
    }
    if (operation && operation.returnType) {
      this.payloadTypeChanged.emit(operation.returnType);
    }

    this.selectedOperationParameterInputs = selectedOperationParameterInputs;
  }
}

export function getOperationFromMember(selectedMember: SchemaMember | null, schema: Schema):
  { operation: Operation, name: QualifiedName, params: Parameter[] } {
  if (isNullOrUndefined(selectedMember)) {
    return {
      operation: null,
      name: null,
      params: []
    };
  }
  return getOperationFromQualifiedName(selectedMember.name, schema);
}

export function getOperationFromQualifiedName(name: QualifiedName, schema: Schema):
  { operation: Operation, name: QualifiedName, params: Parameter[] } {
  const operation = schema.operations.find(o => o.qualifiedName.fullyQualifiedName === name.fullyQualifiedName);
  return {
    operation,
    name: operation.qualifiedName,
    params: operation.parameters
  };
}

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Operation, Schema, SchemaMember} from '../../services/schema';
import {AbstractControl, FormControl, FormGroup, Validators} from '@angular/forms';

@Component({
  selector: 'app-polling-input-config',
  template: `
    <div [formGroup]="config">
      <app-form-row title="Operation" helpText="Select an operation published to Vyne to call periodically">
        <app-schema-member-autocomplete
          appearance="outline"
          label="Operation"
          [schema]="schema"
          (selectedMemberChange)="onOperationSelected($event)"
          schemaMemberType="OPERATION"></app-schema-member-autocomplete>
      </app-form-row>
      <app-form-row title="Schedule"
                    helpText="Define the schedule to poll.  Use a cron job syntax, or select one of the examples provided to get started">
        <mat-form-field appearance="outline">
          <mat-label>Poll schedule</mat-label>
          <input matInput placeholder="Enter or select a cron schedule" aria-label="State" [matAutocomplete]="auto"
                 formControlName="pollSchedule">
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
            <input matInput [placeholder]="parameter.key" [formControl]="parameter.value" [matAutocomplete]="parametersAutoComplete">
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
export class PollingInputConfigComponent {

  config: FormGroup;

  @Output()
  configValueChanged = new EventEmitter<any>();

  @Input()
  schema: Schema;

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
    this.config = new FormGroup({
        operationName: new FormControl('', Validators.required),
        pollSchedule: new FormControl('', Validators.required),
        parameterMap: new FormGroup({})
      }
    );
    this.config.valueChanges.subscribe(e => this.configValueChanged.emit(e));
  }

  onOperationSelected($event: SchemaMember) {
    this.selectedOperation = this.schema.operations.find(o => o.qualifiedName.fullyQualifiedName === $event.name.fullyQualifiedName);
    this.config.get('operationName').setValue($event.name.fullyQualifiedName);
    const selectedOperationParameterInputs: { [key: string]: AbstractControl } = {};
    this.selectedOperation.parameters.forEach(p => {
      const controlName = p.name || p.type.shortDisplayName;
      selectedOperationParameterInputs[controlName] = new FormControl('');
    });
    const parametersFormGroup = new FormGroup(selectedOperationParameterInputs);
    this.config.setControl('parameterMap', parametersFormGroup);
    this.selectedOperationParameterInputs = selectedOperationParameterInputs;
  }
}

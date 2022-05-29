import { Component, EventEmitter, Output } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { QualifiedName, SchemaMember } from '../../services/schema';
import { BaseTransportConfigEditor } from './base-transport-config-editor';
import { PipelineTransportSpec } from '../pipelines.service';
import { ConnectorSummary, ConnectorType } from '../../db-connection-editor/db-importer.service';

@Component({
  selector: 'app-sqs-s3-listener-input-config',
  template: `
    <div [formGroup]="config">
      <app-form-row title="Aws connection name"
                    helpText="Specify aws connection name">
        <app-connection-name-autocomplete
          appearance="outline"
          label="Aws connection name"
          [connections]="connections"
          [connectionType]="connectionType"
          [enabled]="editable"
          [selectConnectionName]="connection"
          (selectedConnectionChange)="onConnectionSelected($event)"
        ></app-connection-name-autocomplete>
      </app-form-row>
      <app-form-row title="SQS Queue Name" helpText="Specify the SQS queue name">
        <mat-form-field appearance="outline">
          <mat-label>Queue Name</mat-label>
          <input matInput formControlName="queueName" required>
        </mat-form-field>
      </app-form-row>
      <app-form-row title="Poll Interval" helpText="Specify the queue poll frequency">
        <mat-form-field appearance="outline">
          <mat-label>Poll Schedule</mat-label>
          <mat-select formControlName="pollSchedule">
            <mat-option *ngFor="let pollOption of pollOptions" [value]="pollOption[1]">{{ pollOption[0]}}</mat-option>
          </mat-select>
          <input matInput formControlName="pollSchedule" required>
        </mat-form-field>
      </app-form-row>
      <app-form-row title="Payload type"
                    helpText="Set the taxi type that defines the payload that will be provided">
        <app-schema-member-autocomplete
          appearance="outline"
          label="Payload type"
          [schema]="schema"
          (selectedMemberChange)="onTypeSelected($event)"
          [enabled]="editable"
          [selectedMemberName]="selectedPayloadTypeName"

          schemaMemberType="TYPE"></app-schema-member-autocomplete>
      </app-form-row>
    </div>
  `
})
export class SqsS3InputConfigComponent extends BaseTransportConfigEditor {
  @Output()
  configValueChanged = new EventEmitter<any>();

  config: FormGroup;
  connectionType: ConnectorType = 'AWS';

  targetTypeName: QualifiedName;
  connection: string;

  selectedPayloadTypeName: QualifiedName;

  pollOptions: [string, string] [] = [
    ['1 second', '* * * * * *'],
    ['5 seconds', '*/5 * * * * *'],
    ['10 seconds', '*/10 * * * * *']
  ];

  constructor() {
    super();
    this.config = new FormGroup({
        connection: new FormControl('', Validators.required),
        queueName: new FormControl('', Validators.required),
        targetTypeName: new FormControl('', Validators.required),
        pollSchedule: new FormControl('', Validators.required)
      }
    );
    this.config.valueChanges.subscribe(e => this.configValueChanged.emit(e));
  }

  onConnectionSelected($event: ConnectorSummary): void {
    if ($event) {
      this.config.get('connection').setValue($event.connectionName);
    }
  }

  updateFormValues(value: PipelineTransportSpec): void {
    this.config.patchValue(value);
    if (value.operationName) {
      this.selectedPayloadTypeName = QualifiedName.from(value.operationName);
    }
  }


  afterEnabledUpdated(value: boolean): void {
    if (value) {
      this.config.enable();
    } else {
      this.config.disable();
    }
  }

  onTypeSelected($event: SchemaMember): void {
    if ($event) {
      this.config.get('targetTypeName').setValue($event.name.fullyQualifiedName);
      this.payloadTypeChanged.emit($event.name);
    }
  }
}

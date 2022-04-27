import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {QualifiedName, Schema, SchemaMember, Type} from '../../services/schema';
import {BaseTransportConfigEditor} from './base-transport-config-editor';
import {PipelineTransportSpec} from '../pipelines.service';
import {ConnectorSummary} from "../../db-connection-editor/db-importer.service";

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
  `,
  styleUrls: ['./http-listener-input-config.component.scss']
})
export class SqsS3InputConfigComponent extends BaseTransportConfigEditor {

  config: FormGroup;
  connectionType = 'AWS';

  targetTypeName: QualifiedName;
  connection: String;

  @Output()
  configValueChanged = new EventEmitter<any>();

  selectedPayloadTypeName: QualifiedName;

  s3Regions = [
    'us-east-2',
    'us-west-1',
    'us-west-2',
    'us-gov-west-1',
    'us-gov-east-1',
    'eu-west-1',
    'eu-west-2',
    'eu-west-3',
    'eu-central-1',
    'eu-north-1',
    'eu-north-1',
    'eu-north-1',
    'ap-southeast-1',
    'ap-southeast-2',
    'ap-northeast-1',
    'ap-northeast-2',
    'ap-south-1',
    'sa-east-1',
    'ca-central-1',
    'cn-north-1',
    'cn-northwest-1',
    'me-south-1',
    'af-south-1'
  ];

  pollOptions: [string, string] [] = [
    ['1 second', '* * * * * *'],
    ['5 seconds', '*/5 * * * * *'],
    ['10 seconds', '*/10 * * * * *']
  ]

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

  onConnectionSelected($event: ConnectorSummary) {
    if ($event) {
      this.config.get('connection').setValue($event.connectionName);
    }
  }

  updateFormValues(value: PipelineTransportSpec) {
    this.config.patchValue(value);
    if (value.operationName) {
      this.selectedPayloadTypeName = QualifiedName.from(value.operationName);
    }
  }


  afterEnabledUpdated(value: boolean) {
    value ? this.config.enable() : this.config.disable();
  }

  onTypeSelected($event: SchemaMember) {
    if ($event) {
      this.config.get('targetTypeName').setValue($event.name.fullyQualifiedName);
      this.payloadTypeChanged.emit($event.name);
    }
  }
}

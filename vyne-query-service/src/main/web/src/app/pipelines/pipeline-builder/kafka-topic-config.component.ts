import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {QualifiedName, Schema, SchemaMember} from '../../services/schema';
import {map} from 'rxjs/operators';
import {bootstrap} from 'angular';
import {PipelineDirection, PipelineTransportSpec} from '../pipelines.service';
import {BaseTransportConfigEditor} from './base-transport-config-editor';

@Component({
  selector: 'app-kafka-topic-config',
  template: `
    <div [formGroup]="config">
      <app-form-row title="Kafka topic"
                    helpText="Specify the Kafka topic to use">
        <mat-form-field appearance="outline">
          <mat-label>Topic</mat-label>
          <input matInput formControlName="topic" required>
        </mat-form-field>
      </app-form-row>
      <app-form-row title="Server address"
                    helpText="Provide the address(es) of the Kafka server to target.  Sometimes called a Bootstrap server in Kafka parlance">
        <mat-form-field appearance="outline">
          <mat-label>Server address</mat-label>
          <input matInput formControlName="bootstrapServer" required>
        </mat-form-field>
      </app-form-row>
      <app-form-row title="Group Id"
                    helpText="The group id defines a set of consumers who will share reading the messages from this topic"
                    *ngIf="direction === 'INPUT'"
      >
        <mat-form-field appearance="outline">
          <mat-label>Group Id</mat-label>
          <input matInput formControlName="groupId">
        </mat-form-field>
      </app-form-row>
      <app-form-row title="Payload type"
                    helpText="Set the taxi type that defines the payload that will be provided">
        <app-schema-member-autocomplete
          appearance="outline"
          label="Payload type"
          [schema]="schema"
          (selectedMemberChange)="onTypeSelected($event)"
          schemaMemberType="TYPE"></app-schema-member-autocomplete>
      </app-form-row>
    </div>
  `,
  styleUrls: ['./kafka-topic-config.component.scss']
})
export class KafkaTopicConfigComponent extends BaseTransportConfigEditor {

  config: FormGroup;

  @Output()
  configValueChanged = new EventEmitter<any>();

  @Input()
  schema: Schema;

  @Input()
  direction: PipelineDirection;

  constructor() {
    super();
    this.config = new FormGroup({
        topic: new FormControl('', Validators.required),
        targetType: new FormControl('', Validators.required),
        bootstrapServer: new FormControl(),
        groupId: new FormControl('vyne-pipeline-runners'),
      }
    );
    this.config.valueChanges
      .pipe(map(e => {
        let props;
        if (this.direction === 'OUTPUT') {
          props = {
            'bootstrap.servers': e.bootstrapServer
          };
        } else {
          props = {
            'bootstrap.servers': e.bootstrapServer,
            'group.id': e.groupId
          };
        }
        return {
          props,
          ...e
        };
      }))
      .subscribe(e => this.configValueChanged.emit(e));
  }


  onTypeSelected($event: SchemaMember) {
    this.config.get('targetType').setValue($event.name.fullyQualifiedName);
  }

  updateFormValues(value: PipelineTransportSpec) {
    this.config.patchValue({
      ...value,
      bootstrapServer: value.props['bootstrap.servers'],
      groupId: value.props['group.id']
    });
  }


  afterEnabledUpdated(value: boolean) {
    value ? this.config.enable() : this.config.disable();
  }
}

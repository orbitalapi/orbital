import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {Schema, SchemaMember} from '../../services/schema';

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
export class KafkaTopicConfigComponent {

  config: FormGroup;

  @Output()
  configValueChanged = new EventEmitter<any>();

  @Input()
  schema: Schema;

  constructor() {
    this.config = new FormGroup({
        topic: new FormControl('', Validators.required),
        targetType: new FormControl('', Validators.required),
        bootstrapServer: new FormControl()
      }
    );
    this.config.valueChanges.subscribe(e => this.configValueChanged.emit(e));
  }


  onTypeSelected($event: SchemaMember) {
    this.config.get('targetType').setValue($event.name.fullyQualifiedName);
  }

}

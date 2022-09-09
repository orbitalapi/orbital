import { Component, EventEmitter, Output } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { QualifiedName, Schema, SchemaMember } from '../../services/schema';
import { BaseTransportConfigEditor } from './base-transport-config-editor';
import { PipelineTransportSpec } from '../pipelines.service';
import { ConnectorSummary } from '../../db-connection-editor/db-importer.service';

@Component({
  selector: 'app-jdbc-output-config',
  template: `
    <div [formGroup]="config">
      <app-form-row title="JDBC connection name"
                    helpText="Specify jdbc connection name">
        <app-connection-name-autocomplete
          appearance="outline"
          label="Jdbc connection name"
          [connections]="connections"
          [connectionType]="connectionType"
          [enabled]="editable"
          [selectConnectionName]="connection"
          (selectedConnectionChange)="onConnectionSelected($event)"
        ></app-connection-name-autocomplete>
      </app-form-row>
      <app-form-row title="Payload type"
                    helpText="Set the taxi type that defines the payload that will be provided">
        <app-schema-member-autocomplete
          appearance="outline"
          label="Payload type"
          [schema]="schema"
          [enabled]="editable"
          [selectedMemberName]="targetTypeName"
          (selectedMemberChange)="onTypeSelected($event)"
          schemaMemberType="TYPE"></app-schema-member-autocomplete>
      </app-form-row>
    </div>
  `
})
export class JdbcOutputConfigComponent extends BaseTransportConfigEditor {
  config: FormGroup;

  @Output()
  configValueChanged = new EventEmitter<any>();

  targetTypeName: QualifiedName;

  connection: string;

  connectionType: 'JDBC';

  constructor() {
    super();
    this.config = new FormGroup({
        connection: new FormControl('', Validators.required),
        targetTypeName: new FormControl('', Validators.required)
      }
    );
    this.config.valueChanges.subscribe(e => this.configValueChanged.emit(e));
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

  onConnectionSelected($event: ConnectorSummary) {
    if ($event) {
      this.config.get('connection').setValue($event.connectionName);
    }
  }

  updateFormValues(value: PipelineTransportSpec, schema: Schema) {
    this.config.patchValue(value);
    if (value.targetTypeName) {
      this.targetTypeName = QualifiedName.from(value.targetTypeName);
    }
  }
}

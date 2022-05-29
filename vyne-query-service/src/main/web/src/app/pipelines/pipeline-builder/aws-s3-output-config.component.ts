import { Component, EventEmitter, Output } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { QualifiedName, SchemaMember } from '../../services/schema';
import { BaseTransportConfigEditor } from './base-transport-config-editor';
import { PipelineTransportSpec } from '../pipelines.service';
import { ConnectorSummary, ConnectorType } from '../../db-connection-editor/db-importer.service';

@Component({
  selector: 'app-aws-s3-output-config',
  templateUrl: './aws-s3-output-config.component.html'
})
export class AwsS3OutputConfigComponent extends BaseTransportConfigEditor {

  @Output()
  configValueChanged = new EventEmitter<any>();

  config: FormGroup;
  connectionType: ConnectorType = 'AWS';

  targetTypeName: QualifiedName;
  connection: string;

  selectedPayloadTypeName: QualifiedName;

  constructor() {
    super();
    this.config = new FormGroup({
        connectionName: new FormControl('', Validators.required),
        bucket: new FormControl('', Validators.required),
        objectKey: new FormControl('', Validators.required),
        targetTypeName: new FormControl('', Validators.required)
      }
    );
    this.config.valueChanges.subscribe(event => this.configValueChanged.emit(event));
  }

  onConnectionSelected(connection: ConnectorSummary): void {
    if (connection) {
      this.config.get('connectionName').setValue(connection.connectionName);
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

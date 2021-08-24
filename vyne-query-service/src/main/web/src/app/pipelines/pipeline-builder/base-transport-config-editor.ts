import {PipelineTransportSpec} from '../pipelines.service';
import {EventEmitter, Input, Output} from '@angular/core';
import {isNullOrUndefined} from 'util';
import {QualifiedName, Schema} from '../../services/schema';

export abstract class BaseTransportConfigEditor {
  private _editable = true;

  @Input()
  get editable(): boolean {
    return this._editable;
  }

  set editable(value: boolean) {
    if (value === this._editable) {
      return;
    }
    this._editable = value;
    this.afterEnabledUpdated(value);
  }

  @Output()
  payloadTypeChanged = new EventEmitter<QualifiedName>();


  private _schema: Schema;

  @Input()
  get schema(): Schema {
    return this._schema;
  }

  set schema(value: Schema) {
    if (value === this._schema) {
      return;
    }
    this._schema = value;
    if (this.pipelineTransportSpec && this.schema) {
      this.updateFormValues(this.pipelineTransportSpec, this.schema);
    }
  }

  private _pipelineTransportSpec: PipelineTransportSpec;

  @Input()
  get pipelineTransportSpec(): PipelineTransportSpec {
    return this._pipelineTransportSpec;
  }

  set pipelineTransportSpec(value: PipelineTransportSpec) {
    if (this._pipelineTransportSpec !== value && !isNullOrUndefined(value)) {
      this._pipelineTransportSpec = value;
      if (this.pipelineTransportSpec && this.schema) {
        this.updateFormValues(this.pipelineTransportSpec, this.schema);
      }
    }
  }


  abstract afterEnabledUpdated(value: boolean);

  abstract updateFormValues(value: PipelineTransportSpec, schema: Schema);
}


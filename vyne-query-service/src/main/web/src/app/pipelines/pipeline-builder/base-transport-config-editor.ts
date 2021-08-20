import {PipelineTransportSpec} from '../pipelines.service';
import {Input} from '@angular/core';
import {isNullOrUndefined} from 'util';

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

  private _pipelineTransportSpec: PipelineTransportSpec;

  @Input()
  get pipelineTransportSpec(): PipelineTransportSpec {
    return this._pipelineTransportSpec;
  }

  set pipelineTransportSpec(value: PipelineTransportSpec) {
    if (this._pipelineTransportSpec !== value && !isNullOrUndefined(value)) {
      this._pipelineTransportSpec = value;
      this.updateFormValues(value);
    }
  }


  abstract afterEnabledUpdated(value: boolean);

  abstract updateFormValues(value: PipelineTransportSpec);
}


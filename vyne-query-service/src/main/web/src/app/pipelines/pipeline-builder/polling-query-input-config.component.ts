import { Component, EventEmitter, Output } from '@angular/core';
import { Schema } from '../../services/schema';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { PipelineTransportSpec } from '../pipelines.service';
import { BaseTransportConfigEditor } from './base-transport-config-editor';

@Component({
  selector: 'app-polling-query-input-config',
  templateUrl: './polling-query-input-config.component.html',
  styleUrls: ['./polling-query-input-config.component.scss']
})
export class PollingQueryInputConfigComponent extends BaseTransportConfigEditor {

  config: UntypedFormGroup;

  @Output()
  configValueChanged = new EventEmitter<any>();

  constructor() {
    super();
    this.config = new UntypedFormGroup({
        query: new UntypedFormControl('', Validators.required),
        pollSchedule: new UntypedFormControl('', Validators.required)
      }
    );
    this.config.valueChanges.subscribe(change => this.configValueChanged.emit(change));
  }

  updateFormValues(value: PipelineTransportSpec, schema: Schema) {
    this.config.patchValue(value);
  }

  afterEnabledUpdated(value: boolean) {
    value ? this.config.enable() : this.config.disable();
  }
}

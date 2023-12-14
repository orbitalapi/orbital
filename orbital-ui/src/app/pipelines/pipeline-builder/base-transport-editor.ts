import {PipelineTransport, PipelineTransportSpec} from '../pipelines.service';
import {AbstractControl, UntypedFormGroup} from '@angular/forms';
import { Input, Directive } from '@angular/core';
import {Schema} from '../../services/schema';
import {BaseTransportConfigEditor} from './base-transport-config-editor';

@Directive()
export class BaseTransportEditorComponent extends BaseTransportConfigEditor {
  @Input()
  pipelineTransportSpecFg: UntypedFormGroup;

  @Input()
  label: string;

  @Input()
  transportSpecType: string;

  pipelineTransportLabel(transport: PipelineTransport): string {
    return transport ? transport.label : '';
  }

  updateConfigValue(config: any) {
    this.pipelineTransportSpecFg.patchValue(config);
  }

  updateFormValues(value: PipelineTransportSpec) {
  }

  afterEnabledUpdated(value: boolean) {
  }
}


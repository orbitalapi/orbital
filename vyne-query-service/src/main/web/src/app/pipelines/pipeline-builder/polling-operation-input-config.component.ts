import { Component, EventEmitter, Output } from '@angular/core';
import { Operation, Parameter, QualifiedName, Schema, SchemaMember } from '../../services/schema';
import { AbstractControl, FormControl, FormGroup, Validators } from '@angular/forms';
import { PipelineTransportSpec } from '../pipelines.service';
import { BaseTransportConfigEditor } from './base-transport-config-editor';
import { getOperationFromMember, getOperationFromQualifiedName } from './schema-helpers';

@Component({
  selector: 'app-polling-operation-input-config',
  templateUrl: './polling-operation-input-config.component.html',
  styleUrls: ['./polling-operation-input-config.component.scss']
})
export class PollingOperationInputConfigComponent extends BaseTransportConfigEditor {
  @Output()
  configValueChanged = new EventEmitter<any>();

  config: FormGroup;

  selectedOperationName: QualifiedName;
  selectedOperation: Operation;
  selectedOperationParameterInputs: { [key: string]: AbstractControl };

  pipelineParameters = [
    { label: 'The last time this pipeline poll completed', value: '\$pipeline.lastRunTime' },
    { label: 'The current time', value: '\$env.now' }
  ];

  constructor() {
    super();
    this.config = new FormGroup({
        operationName: new FormControl('', Validators.required),
        pollSchedule: new FormControl('', Validators.required),
        parameterMap: new FormGroup({})
      }
    );
    this.config.valueChanges.subscribe(e => this.configValueChanged.emit(e));
  }

  updateFormValues(value: PipelineTransportSpec, schema: Schema): void {

    // Set up the operationName before patching the value, as this configures
    // the FormControls for the operation params
    if (value.operationName) {
      this.selectedOperationName = QualifiedName.from(value.operationName);
      const { operation, name, params } = getOperationFromQualifiedName(this.selectedOperationName, schema);
      this.handleSelectedOperationUpdated(name, operation, params);
    }

    this.config.patchValue(value);
  }

  afterEnabledUpdated(value: boolean): void {
    value ? this.config.enable() : this.config.disable();
  }

  onOperationSelected($event: SchemaMember): void {
    const { operation, name, params } = getOperationFromMember($event, this.schema);
    if (this.selectedOperationName && this.selectedOperationName.fullyQualifiedName === name.fullyQualifiedName) {
      // Nothing has changed, so bail
      return;
    }
    this.handleSelectedOperationUpdated(name, operation, params);
  }

  private handleSelectedOperationUpdated(name: QualifiedName, operation: Operation, params: Parameter[]): void {
    const fullyQualifiedName: string = name ? name.fullyQualifiedName : null;

    this.selectedOperation = operation;
    this.selectedOperationName = name;
    this.config.get('operationName').setValue(fullyQualifiedName);
    const selectedOperationParameterInputs: { [key: string]: AbstractControl } = {};
    params.forEach(p => {
      const controlName = p.name || p.typeName.shortDisplayName;
      selectedOperationParameterInputs[controlName] = new FormControl('');
    });
    const parametersFormGroup = new FormGroup(selectedOperationParameterInputs);
    this.config.setControl('parameterMap', parametersFormGroup);
    if (!this.editable) {
      parametersFormGroup.disable();
    }
    if (operation && operation.returnTypeName) {
      this.payloadTypeChanged.emit(operation.returnTypeName);
    }

    this.selectedOperationParameterInputs = selectedOperationParameterInputs;
  }
}

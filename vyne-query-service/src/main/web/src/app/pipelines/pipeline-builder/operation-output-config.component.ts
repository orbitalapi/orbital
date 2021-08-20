import {Component, EventEmitter, Input, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {Operation, QualifiedName, Schema, SchemaMember} from '../../services/schema';
import {BaseTransportConfigEditor} from './base-transport-config-editor';
import {PipelineTransportSpec} from '../pipelines.service';
import {getOperationFromMember} from './polling-input-config.component';

@Component({
  selector: 'app-operation-output-config',
  template: `
    <div [formGroup]="config">
      <app-form-row title="Operation"
                    helpText="Select an operation from your schema to publish the messages to.  Vyne will transform the message if necessary">
        <app-schema-member-autocomplete
          appearance="outline"
          label="Operation"
          [schema]="schema"
          [enabled]="editable"
          [selectedMemberName]="selectedOperationName"
          (selectedMemberChange)="onOperationSelected($event)"
          schemaMemberType="OPERATION"></app-schema-member-autocomplete>
      </app-form-row>
    </div>
  `,
  styleUrls: ['./operation-output-config.component.scss']
})
export class OperationOutputConfigComponent extends BaseTransportConfigEditor {

  config: FormGroup;

  @Output()
  configValueChanged = new EventEmitter<any>();

  @Input()
  schema: Schema;

  selectedOperation: Operation;
  selectedOperationName: QualifiedName;

  constructor() {
    super();
    this.config = new FormGroup({
        operationName: new FormControl('', Validators.required),
        schedule: new FormControl('', Validators.required),
        parameterMap: new FormGroup({})
      }
    );
    this.config.valueChanges.subscribe(e => this.configValueChanged.emit(e));
  }

  afterEnabledUpdated(value: boolean) {
    value ? this.config.enable() : this.config.disable();
  }

  updateFormValues(value: PipelineTransportSpec) {
    this.config.patchValue(value);
    if (value.operationName) {
      this.selectedOperationName = QualifiedName.from(value.operationName);
    }
  }

  onOperationSelected($event: SchemaMember) {
    const {operation, name, params} = getOperationFromMember($event, this.schema);
    const fullyQualifiedName: string = name ? name.fullyQualifiedName : null;
    this.selectedOperation = operation;
    this.selectedOperationName = name;
    this.config.get('operationName').setValue(fullyQualifiedName);
  }
}

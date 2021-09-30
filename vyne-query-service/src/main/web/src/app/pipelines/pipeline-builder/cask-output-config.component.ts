import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {QualifiedName, Schema, SchemaMember} from '../../services/schema';
import {BaseTransportConfigEditor} from './base-transport-config-editor';
import {PipelineTransportSpec} from '../pipelines.service';
import {getOperationFromMember} from './polling-input-config.component';

@Component({
  selector: 'app-cask-output-config',
  template: `
    <app-form-row title="Target type"
                  helpText="Set the taxi type that defines the data to store in the cask">
      <app-schema-member-autocomplete
        appearance="outline"
        label="Target type"
        [schema]="schema"
        [enabled]="editable"
        [selectedMemberName]="selectedOperationName"
        (selectedMemberChange)="onTypeSelected($event)"
        schemaMemberType="TYPE"></app-schema-member-autocomplete>
    </app-form-row>
  `,
  styleUrls: ['./cask-output-config.component.scss']
})
export class CaskOutputConfigComponent extends BaseTransportConfigEditor {
  config: FormGroup;

  @Output()
  configValueChanged = new EventEmitter<any>();

  selectedOperationName: QualifiedName;

  constructor() {
    super();
    this.config = new FormGroup({
        targetType: new FormControl('', Validators.required),
      }
    );
    this.config.valueChanges.subscribe(e => this.configValueChanged.emit(e));
  }

  updateFormValues(value: PipelineTransportSpec, schema: Schema) {
    this.config.patchValue(value);
    if (value.operationName) {
      this.selectedOperationName = QualifiedName.from(value.operationName);
    }
  }

  afterEnabledUpdated(value: boolean) {
    value ? this.config.enable() : this.config.disable();
  }

  onTypeSelected($event: SchemaMember) {
    const {operation, name, params} = getOperationFromMember($event, this.schema);
    const fullyQualifiedName: string = name ? name.fullyQualifiedName : null;
    this.selectedOperationName = name;
    this.config.get('targetType').setValue(fullyQualifiedName);
  }
}

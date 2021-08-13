import {Component, EventEmitter, Input, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {Operation, Schema, SchemaMember} from '../../services/schema';

@Component({
  selector: 'app-operation-output-config',
  template: `
    <div [formGroup]="config">
      <app-form-row title="Operation" helpText="Select an operation published to Vyne to call periodically">
        <app-schema-member-autocomplete
          appearance="outline"
          label="Operation"
          [schema]="schema"
          (selectedMemberChange)="onOperationSelected($event)"
          schemaMemberType="OPERATION"></app-schema-member-autocomplete>
      </app-form-row>
    </div>
  `,
  styleUrls: ['./operation-output-config.component.scss']
})
export class OperationOutputConfigComponent {

  config: FormGroup;

  @Output()
  configValueChanged = new EventEmitter<any>();

  @Input()
  schema: Schema;

  selectedOperation: Operation;

  constructor() {
    this.config = new FormGroup({
        operationName: new FormControl('', Validators.required),
        schedule: new FormControl('', Validators.required),
        parameterMap: new FormGroup({})
      }
    );
    this.config.valueChanges.subscribe(e => this.configValueChanged.emit(e));
  }

  onOperationSelected($event: SchemaMember) {
    this.selectedOperation = this.schema.operations.find(o => o.qualifiedName.fullyQualifiedName === $event.name.fullyQualifiedName);
    this.config.get('operationName').setValue($event.name.fullyQualifiedName);
  }
}

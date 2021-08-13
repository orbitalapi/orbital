import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {Schema, SchemaMember} from '../../services/schema';

@Component({
  selector: 'app-cask-output-config',
  template: `
    <app-form-row title="Target type"
                  helpText="Set the taxi type that defines the data to store in the cask">
      <app-schema-member-autocomplete
        appearance="outline"
        label="Target type"
        [schema]="schema"
        (selectedMemberChange)="onTypeSelected($event)"
        schemaMemberType="TYPE"></app-schema-member-autocomplete>
    </app-form-row>
  `,
  styleUrls: ['./cask-output-config.component.scss']
})
export class CaskOutputConfigComponent {
  config: FormGroup;

  @Input()
  schema: Schema;

  @Output()
  configValueChanged = new EventEmitter<any>();

  constructor() {
    this.config = new FormGroup({
        targetType: new FormControl('', Validators.required),
      }
    );
    this.config.valueChanges.subscribe(e => this.configValueChanged.emit(e));
  }

  onTypeSelected($event: SchemaMember) {
    this.config.get('targetType').setValue($event.name.fullyQualifiedName);
  }
}

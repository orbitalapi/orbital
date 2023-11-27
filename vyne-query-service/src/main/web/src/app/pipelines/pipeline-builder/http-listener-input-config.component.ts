import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {UntypedFormControl, UntypedFormGroup, Validators} from '@angular/forms';
import {QualifiedName, Schema, SchemaMember, Type} from '../../services/schema';
import {BaseTransportConfigEditor} from './base-transport-config-editor';
import {PipelineTransportSpec} from '../pipelines.service';

@Component({
  selector: 'app-http-listener-input-config',
  template: `
    <div [formGroup]="config">
      <app-form-row title="API Endpoint"
                    helpText="Define an endpoint which other services can call to trigger this pipeline">
        <mat-form-field appearance="outline">
          <mat-label>Path</mat-label>
          <input matInput formControlName="path" required>
        </mat-form-field>
      </app-form-row>
      <app-form-row title="HTTP Method" helpText="Specify the HTTP method to expose">
        <mat-form-field appearance="outline">
          <mat-label>HTTP Method</mat-label>
          <mat-select formControlName="method">
            <mat-option *ngFor="let httpMethod of httpMethods" [value]="httpMethod">{{ httpMethod}}</mat-option>
          </mat-select>
          <input matInput formControlName="path" required>
        </mat-form-field>
      </app-form-row>
      <app-form-row title="Payload type"
                    helpText="Set the taxi type that defines the payload that will be provided">
        <app-schema-member-autocomplete
          appearance="outline"
          label="Payload type"
          [schema]="schema"
          (selectedMemberChange)="onTypeSelected($event)"
          [enabled]="editable"
          [selectedMemberName]="selectedPayloadTypeName"

          schemaMemberType="TYPE"></app-schema-member-autocomplete>
      </app-form-row>
    </div>
  `,
  styleUrls: ['./http-listener-input-config.component.scss']
})
export class HttpListenerInputConfigComponent extends BaseTransportConfigEditor {

  config: UntypedFormGroup;

  @Output()
  configValueChanged = new EventEmitter<any>();

  selectedPayloadTypeName: QualifiedName;

  httpMethods = ['GET', 'POST', 'PUT'];

  constructor() {
    super();
    this.config = new UntypedFormGroup({
        path: new UntypedFormControl('', Validators.required),
        method: new UntypedFormControl('', Validators.required),
        payloadType: new UntypedFormControl()
      }
    );
    this.config.valueChanges.subscribe(e => this.configValueChanged.emit(e));
  }

  updateFormValues(value: PipelineTransportSpec) {
    this.config.patchValue(value);
    if (value.operationName) {
      this.selectedPayloadTypeName = QualifiedName.from(value.operationName);
    }
  }


  afterEnabledUpdated(value: boolean) {
    value ? this.config.enable() : this.config.disable();
  }

  onTypeSelected($event: SchemaMember) {
    if ($event !== null) {
      this.config.get('payloadType').setValue($event.name.fullyQualifiedName);
      this.selectedPayloadTypeName = $event.name;
      this.payloadTypeChanged.emit($event.name);
    } else {
      this.selectedPayloadTypeName = null;
      this.config.get('payloadType').setValue(null);
    }

  }


}

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {Schema, SchemaMember, Type} from '../../services/schema';

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
          label="Operation"
          [schema]="schema"
          (selectedMemberChange)="onTypeSelected($event)"
          schemaMemberType="TYPE"></app-schema-member-autocomplete>
      </app-form-row>
    </div>
  `,
  styleUrls: ['./http-listener-input-config.component.scss']
})
export class HttpListenerInputConfigComponent {

  config: FormGroup;

  @Output()
  configValueChanged = new EventEmitter<any>();

  @Input()
  schema: Schema;

  httpMethods = ['GET', 'POST', 'PUT'];

  constructor() {
    this.config = new FormGroup({
        path: new FormControl('', Validators.required),
        method: new FormControl('', Validators.required),
        payloadType: new FormControl()
      }
    );
    this.config.valueChanges.subscribe(e => this.configValueChanged.emit(e));
  }


  onTypeSelected($event: SchemaMember) {
    this.config.get('payloadType').setValue($event.name.fullyQualifiedName);
  }


}

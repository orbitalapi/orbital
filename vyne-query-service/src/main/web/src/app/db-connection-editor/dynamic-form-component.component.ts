import {Component, Input, OnInit} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {MatFormFieldAppearance} from '@angular/material';

export class DynamicFormComponentSpec {
  constructor(readonly componentType: ComponentType,
              readonly key: string,
              readonly label: string,
              readonly required: boolean,
              readonly inputType: InputType | null,
              public value: any = null) {
  }
}

export type InputType = 'text' | 'password' | 'number';
export type ComponentType = 'input' | 'checkbox';


@Component({
  selector: 'app-dyanmic-form-component',
  template: `
    <div [formGroup]="form">
      <mat-form-field [appearance]="appearance">
        <mat-label>{{spec.label}}</mat-label>
        <div [ngSwitch]="spec.componentType">
          <input matInput *ngSwitchCase="'input'" [formControlName]="spec.key"
                 [id]="spec.key" [type]="spec.inputType">
        </div>
        <mat-error *ngIf="!isValid">{{spec.label}} is required</mat-error>
      </mat-form-field>
    </div>
  `,
  styleUrls: ['./dynamic-form-component.component.scss']
})
export class DynamicFormComponentComponent {

  @Input() form!: FormGroup;
  @Input() spec: DynamicFormComponentSpec;

  @Input() appearance: MatFormFieldAppearance = 'outline';

  get isValid() {
    return this.form.controls[this.spec.key].valid;
  }
}


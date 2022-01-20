import {Component, Input, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {MatFormFieldAppearance} from '@angular/material/form-field';
import {TuiInputModeT, TuiInputTypeT} from '@taiga-ui/cdk';

export class DynamicFormComponentSpec {
  constructor(readonly componentType: ComponentType,
              readonly key: string,
              readonly label: string,
              readonly required: boolean,
              readonly textFieldMode: TuiInputModeT | null,
              readonly textFieldType: TuiInputTypeT | null,
              public value: any = null) {
  }
}

export type InputType = 'text' | 'password' | 'number';
export type ComponentType = 'input' | 'checkbox';


@Component({
  selector: 'app-dyanmic-form-component',
  template: `
    <div [formGroup]="form" class="tui-form-field">

      <div [ngSwitch]="spec.componentType">
        <tui-input *ngSwitchCase="'input'" [formControlName]="spec.key"
                   [tuiTextfieldInputMode]="spec.textFieldMode"
                   [tuiTextfieldType]="spec.textFieldType">{{spec.label}}</tui-input>
      </div>
      <tui-field-error [formControlName]="spec.key" [required]="spec.required"></tui-field-error>
    </div>
  `,
  styleUrls: ['./dynamic-form-component.component.scss']
})
export class DynamicFormComponentComponent {

  @Input() form!: FormGroup;
  @Input() spec: DynamicFormComponentSpec;

  @Input() appearance: MatFormFieldAppearance = 'outline';

  get formControl(): FormControl {
    return this.form.controls[this.spec.key] as FormControl;
  }

  get isValid() {
    return this.form.controls[this.spec.key].valid;
  }
}


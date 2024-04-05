import { AsyncPipe, CommonModule, NgSwitch } from '@angular/common';
import {Component, forwardRef, Input} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
  UntypedFormControl,
  UntypedFormGroup
} from '@angular/forms';
import {
  TuiCheckboxLabeledModule,
  TuiFieldErrorPipeModule,
  TuiInputModule,
  TuiInputNumberModule,
  TuiInputPasswordModule
} from '@taiga-ui/kit';
import { TUI_NUMBER_FORMAT, TuiErrorModule } from '@taiga-ui/core';

export class DynamicFormComponentSpec {
  constructor(readonly key: string,
              readonly label: string,
              readonly required: boolean,
              readonly inputType: InputType,
              public value: any = null) {
  }
}

export type InputType = 'text' | 'password' | 'number' | 'checkbox';


@Component({
  selector: 'app-dynamic-form-component',
  // We have to disable the thousand seperator, otherwise things like "port" get
  // formatted as 5,442
  providers: [
    { provide: TUI_NUMBER_FORMAT, useValue: { thousandSeparator: '' } },
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DynamicFormComponentComponent),
      multi: true,
    },
  ],
  host: {
    '(blur)': '_onTouched()'
  },
  template: `
    <div [formGroup]="form" class="tui-form-field">

      <ng-container [ngSwitch]="spec.inputType">
        <tui-input *ngSwitchCase="'text'" [formControlName]="spec.key">{{ spec.label }}</tui-input>
        <tui-input-password *ngSwitchCase="'password'" [formControlName]="spec.key">{{ spec.label }}
        </tui-input-password>
        <tui-input-number *ngSwitchCase="'number'" [formControlName]="spec.key">{{ spec.label }}</tui-input-number>
        <tui-checkbox-labeled *ngSwitchCase="'checkbox'" [formControlName]="spec.key">{{ spec.label }}
        </tui-checkbox-labeled>
      </ng-container>
      <tui-error [formControlName]="spec.key" [required]="spec.required" [formGroup]="form"
                 [error]="[] | tuiFieldError | async"
      ></tui-error>
    </div>
  `,
  styleUrls: ['./dynamic-form-component.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TuiInputModule,
    TuiInputPasswordModule,
    TuiInputNumberModule,
    TuiCheckboxLabeledModule,
    TuiErrorModule,
    TuiFieldErrorPipeModule,
    AsyncPipe
  ],
  standalone: true
})
export class DynamicFormComponentComponent implements ControlValueAccessor {
  _onTouched: any;

  @Input() form!: UntypedFormGroup;
  @Input() spec: DynamicFormComponentSpec;

  get formControl(): UntypedFormControl {
    return this.form.controls[this.spec.key] as UntypedFormControl;
  }

  get isValid() {
    return this.form.controls[this.spec.key].valid;
  }

  writeValue(obj: any): void {
    this.formControl.setValue(obj)
  }

  registerOnChange(fn: any): void {
    this.formControl.registerOnChange(fn)
  }

  registerOnTouched(fn: any): void {
    this._onTouched = fn
  }

  setDisabledState?(isDisabled: boolean): void {
    if (this.formControl.disabled !== isDisabled) {
      if (isDisabled) {
        this.formControl.disable()
      } else {
        this.formControl.enable()
      }
    }
  }

}


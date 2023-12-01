import { Directive } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, ValidationErrors, Validator } from '@angular/forms';
import { isNullOrUndefined } from 'src/app/utils/utils';

const pattern = new RegExp(/\d+\.\d+\.\d+/);

@Directive({
  selector: '[semver]',
  providers: [{ provide: NG_VALIDATORS, useExisting: SemverValidatorDirective, multi: true }]
})
export class SemverValidatorDirective implements Validator {

  validate(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (isNullOrUndefined(value)) {
      return {
        required: value
      }
    }
    const testResult = pattern.test(control.value);
    if (!testResult) {
      return {
        invalidSemver: value
      }
    }
    return null;
  }

}

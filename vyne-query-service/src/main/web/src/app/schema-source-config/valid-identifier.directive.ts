import { Directive } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, ValidationErrors, Validator } from '@angular/forms';
import { isNullOrUndefined } from 'util';


// Start with a letter, then any collection of letters or digits.
const pattern = new RegExp('[a-zA-Z]\\w*');

@Directive({
  selector: '[validIdentifier]',
  providers: [{ provide: NG_VALIDATORS, useExisting: ValidIdentifierDirective, multi: true }]
})
export class ValidIdentifierDirective implements Validator {


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
  }

}

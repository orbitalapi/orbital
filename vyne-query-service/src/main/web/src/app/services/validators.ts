import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

const typeNameRegex = new RegExp('^[a-zA-Z]\\w*$'); // Any character, following by any character or number
const namespaceRegex = new RegExp('^[a-zA-Z][\\w\\.]*$'); // Any character, following by any character or number or dot

export function validTypeName(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const isValid = typeNameRegex.test(control.value);
    return isValid ? null : {invalidTypeName: {value: control.value}};
  };
}

export function validNamespace(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const isValid = namespaceRegex.test(control.value);
    return isValid ? null : {invalidNamespace: {value: control.value}};
  };
}

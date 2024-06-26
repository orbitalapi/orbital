import {AbstractControl, ValidatorFn} from '@angular/forms';

export function fileExtensionValidator(allowedExtensions: string[]): ValidatorFn {
  return (control: AbstractControl): { [key: string]: any } | null => {
    if (!control.value) {
      return null; // Don't validate empty values to allow required validator to catch them
    }
    const fileExtension = control.value.split('.').pop().toLowerCase();
    const isValidExtension = allowedExtensions.includes(fileExtension);
    return isValidExtension ? null : { invalidFileExtension: { value: control.value } };
  };
}

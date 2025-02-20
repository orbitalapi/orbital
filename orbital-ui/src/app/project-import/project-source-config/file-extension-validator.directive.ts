import {Directive, HostListener, Input} from '@angular/core';
import {AbstractControl, NG_VALIDATORS, ValidationErrors, Validator} from '@angular/forms';

@Directive({
  selector: '[appFileExtensionValidator]',
  standalone: true,
  providers: [
    { provide: NG_VALIDATORS, useExisting: FileExtensionValidatorDirective, multi: true }
  ]
})
export class FileExtensionValidatorDirective implements Validator {
  @Input('appFileExtensionValidator') allowedExtensions: string[] = []; // eg. ['.json', '.yaml']

  validate(control: AbstractControl): ValidationErrors | null {
    const value: string = control.value;
    const errorValue = { invalidFileExtension: true };
    if (!value) {
      return null;
    }

    if (!value.includes('.')) {
      // no extension provided - is that allowed?
      if (this.allowedExtensions.includes('')) {
        return null
      } else {
        return errorValue
      }
    }
    const extension = value.substring(value.lastIndexOf('.'));
    if (this.allowedExtensions.includes(extension)) {
      return null;
    }



    return errorValue;
  }

  @HostListener('input', ['$event'])
  onInput(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    const value = inputElement.value;
    const extension = value.substring(value.lastIndexOf('.'));

    if (!this.allowedExtensions.includes(extension)) {
      inputElement.setCustomValidity(`Invalid file extension. Allowed extensions are: ${this.allowedExtensions.join(', ')}`);
    } else {
      inputElement.setCustomValidity('');
    }
  }
}


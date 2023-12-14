import {Directive, Input} from '@angular/core';
import {FormControl, NgControl} from "@angular/forms";

/**
 * Binding expression which disables form controls
 * From: https://netbasal.com/disabling-form-controls-when-working-with-reactive-forms-in-angular-549dd7b42110
 */
@Directive({
  selector: '[disableControl]'
})
export class DisableControlDirective {

  @Input() set disableControl(condition: boolean) {
    if (condition) {
      this.ngControl.control.disable();
    } else {
      this.ngControl.control.enable();
    }
  }

  constructor(private ngControl: NgControl) {
  }
}

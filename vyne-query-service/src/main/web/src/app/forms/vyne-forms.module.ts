import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormRowComponent} from './form-row.component';
import {MatLegacyInputModule as MatInputModule} from '@angular/material/legacy-input';
import {MatLegacyFormFieldModule as MatFormFieldModule} from '@angular/material/legacy-form-field';


@NgModule({
  declarations: [FormRowComponent],
  exports: [
    FormRowComponent
  ],
  imports: [
    CommonModule,
    MatFormFieldModule,
  ]
})
export class VyneFormsModule {
}

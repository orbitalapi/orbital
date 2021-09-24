import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormRowComponent} from './form-row.component';
import {MatInputModule} from '@angular/material/input';
import {MatFormFieldModule} from '@angular/material/form-field';


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

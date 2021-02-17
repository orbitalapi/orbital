import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {TestSpecFormComponent} from './test-spec-form.component';
import {MatFormFieldModule} from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';



@NgModule({
  declarations: [
    TestSpecFormComponent
  ],
  imports: [
    CommonModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    ReactiveFormsModule
  ],
  entryComponents: [
    TestSpecFormComponent
  ]
})
export class TestPackModuleModule { }

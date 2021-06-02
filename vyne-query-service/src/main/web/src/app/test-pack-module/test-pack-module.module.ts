import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {TestSpecFormComponent} from './test-spec-form.component';
import {MatFormFieldModule} from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';



@NgModule({
  declarations: [
    TestSpecFormComponent
  ],
    imports: [
        CommonModule,
        MatFormFieldModule,
        MatInputModule,
        FormsModule,
        ReactiveFormsModule,
        MatButtonModule
    ],
  entryComponents: [
    TestSpecFormComponent
  ]
})
export class TestPackModuleModule { }

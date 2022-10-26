import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TestSpecFormComponent} from './test-spec-form.component';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {ConfigDisabledFormComponent} from './config-disabled-form.component';
import {ConfigPersistResultsDisabledFormComponent} from './config-persist-results-disabled-form.component';
import { MatToolbarModule } from '@angular/material/toolbar';


@NgModule({
  declarations: [
    TestSpecFormComponent,
    ConfigDisabledFormComponent,
    ConfigPersistResultsDisabledFormComponent
  ],
  imports: [
    CommonModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatToolbarModule
  ],
  entryComponents: [
    TestSpecFormComponent,
    ConfigDisabledFormComponent,
    ConfigPersistResultsDisabledFormComponent
  ]
})
export class TestPackModuleModule {
}

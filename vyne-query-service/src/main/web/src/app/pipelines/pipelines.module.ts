import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {PipelineWizardComponent} from './pipeline-wizard/pipeline-wizard.component';
import {AppModule} from '../app.module';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {MatFormFieldModule} from '@angular/material/form-field';
import {FormsModule} from '@angular/forms';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatButtonModule} from '@angular/material/button';

@NgModule({
  declarations: [PipelineWizardComponent],
  imports: [
    CommonModule,
    TypeAutocompleteModule,
    MatSlideToggleModule,
    MatButtonModule
  ],
  exports: [
    PipelineWizardComponent
  ]
})
export class PipelinesModule {
}

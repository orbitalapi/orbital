import {NgModule} from '@angular/core';

import {ModelWizardComponent} from './model-wizard.component';
import { AttributePanelComponent } from './attribute-panel.component';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {FormsModule} from '@angular/forms';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';

@NgModule({
  imports: [
    CommonModule,
    BrowserModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    TypeAutocompleteModule
  ],
  exports: [AttributePanelComponent],
  declarations: [ModelWizardComponent, AttributePanelComponent],
  providers: [],
})
export class ModelWizardModule {
}

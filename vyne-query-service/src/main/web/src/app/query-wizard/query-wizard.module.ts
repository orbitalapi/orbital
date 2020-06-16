import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {QueryWizardComponent} from './query-wizard.component';
import {FileFactSelectorComponent} from './file-fact-selector/file-fact-selector.component';
import {ResultViewerModule} from './result-display/result-viewer.module';
import {SearchModule} from '../search/search.module';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatSelectModule} from '@angular/material/select';
import {MatCardModule} from '@angular/material/card';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {VyneQueryViewerComponent} from './taxi-viewer/vyne-query-viewer/vyne-query-viewer.component';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatIconModule} from '@angular/material/icon';
import {CovalentDynamicFormsModule} from '@covalent/dynamic-forms';
import {CovalentFileModule} from '@covalent/core/file';
import {ObjectViewModule} from '../object-view/object-view.module';
import {CovalentHighlightModule} from '@covalent/highlight';
import {MatButtonModule} from '@angular/material/button';

@NgModule({
  imports: [
    BrowserModule,
    CommonModule,
    ResultViewerModule,
    SearchModule,
    TypeAutocompleteModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatButtonModule,
    MatSelectModule,
    MatCardModule,
    MatToolbarModule,
    MatIconModule,
    CovalentDynamicFormsModule,
    FormsModule,
    ReactiveFormsModule,
    CovalentFileModule,
    ObjectViewModule,
    CovalentHighlightModule,
  ],
  exports: [QueryWizardComponent],
  declarations: [QueryWizardComponent, FileFactSelectorComponent, VyneQueryViewerComponent],
  providers: [],
})
export class QueryWizardModule {
}

import {NgModule} from '@angular/core';

import {DbConnectionEditorComponent} from './db-connection-editor.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatCardModule} from '@angular/material/card';
import {MatSelectModule} from '@angular/material/select';
import {MatInputModule} from '@angular/material/input';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {TableSelectorComponent} from './table-selector.component';
import {TableImporterComponent} from './table-importer.component';
import {AgGridAngular, AgGridModule} from 'ag-grid-angular';
import {TypeSelectorCellEditorComponent} from './type-selector-cell-editor.component';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {CheckboxCellEditorComponent} from './checkbox-cell-editor.component';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {
  MatFormFieldAppearance,
  MAT_FORM_FIELD_DEFAULT_OPTIONS,
  MatFormFieldDefaultOptions
} from '@angular/material';
import {CovalentDynamicFormsModule} from '@covalent/dynamic-forms';
import {DynamicFormComponentComponent} from './dynamic-form-component.component';
import {ConnectionTypeSelectorComponent} from './connection-type-selector.component';
import {DbConnectionWizardComponent} from './db-connection-wizard.component';
import {MatStepperModule} from '@angular/material/stepper';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {RouterModule} from '@angular/router';
import {TableSelectorContainerComponent} from './table-selector-container.component';
import {MatMenuModule} from '@angular/material/menu';
import {TableImporterContainerComponent} from './table-importer-container.component';
import {MatDialogModule} from '@angular/material/dialog';
import {TypedEditorModule} from '../type-editor/type-editor.module';

const appearance: MatFormFieldDefaultOptions = {
  appearance: 'outline'
};

@NgModule({
  imports: [
    CommonModule,
    BrowserModule,
    BrowserAnimationsModule,
    MatDialogModule,
    MatCardModule,
    MatSelectModule,
    MatInputModule,
    FormsModule,
    MatButtonModule,
    AgGridModule.withComponents([TypeSelectorCellEditorComponent, CheckboxCellEditorComponent]),
    TypeAutocompleteModule,
    MatCheckboxModule,
    CovalentDynamicFormsModule,
    ReactiveFormsModule,
    MatStepperModule,
    MatProgressBarModule,
    TypedEditorModule,
    RouterModule,
    MatMenuModule],
  exports: [DbConnectionEditorComponent, TableImporterComponent,
    ConnectionTypeSelectorComponent, DbConnectionWizardComponent,
    TableSelectorComponent],
  declarations: [DbConnectionEditorComponent,
    TableSelectorComponent,
    TableImporterComponent,
    TypeSelectorCellEditorComponent,
    CheckboxCellEditorComponent,
    DynamicFormComponentComponent,
    ConnectionTypeSelectorComponent,
    DbConnectionWizardComponent,
    TableSelectorContainerComponent,
    TableImporterContainerComponent,
  ],
  providers: [
    {
      provide: MAT_FORM_FIELD_DEFAULT_OPTIONS,
      useValue: appearance
    }
  ],
})
export class DbConnectionEditorModule {
}

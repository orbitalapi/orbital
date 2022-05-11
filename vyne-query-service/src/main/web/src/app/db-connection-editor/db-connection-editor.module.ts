import {NgModule} from '@angular/core';

import {ConnectionEditorComponent} from './connection-editor.component';
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
import {AgGridModule} from 'ag-grid-angular';
import {TypeSelectorCellEditorComponent} from './type-selector-cell-editor.component';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {CheckboxCellEditorComponent} from './checkbox-cell-editor.component';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {CovalentDynamicFormsModule} from '@covalent/dynamic-forms';
import {DynamicFormComponentComponent} from './dynamic-form-component.component';
import {DbConnectionWizardComponent} from './db-connection-wizard.component';
import {MatStepperModule} from '@angular/material/stepper';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {RouterModule} from '@angular/router';
import {TableSelectorContainerComponent} from './table-selector-container.component';
import {MatMenuModule} from '@angular/material/menu';
import {TableImporterContainerComponent} from './table-importer-container.component';
import {MatDialogModule} from '@angular/material/dialog';
import {TypedEditorModule} from '../type-editor/type-editor.module';
import {MAT_FORM_FIELD_DEFAULT_OPTIONS, MatFormFieldDefaultOptions} from '@angular/material/form-field';
import {
  TuiComboBoxModule,
  TuiDataListWrapperModule,
  TuiFieldErrorModule,
  TuiFilterByInputPipeModule,
  TuiInputModule,
  TuiStringifyContentPipeModule
} from '@taiga-ui/kit';
import {TuiButtonModule, TuiTextfieldControllerModule} from '@taiga-ui/core';
import {DbConnectionEditorDialogComponent} from './db-connection-editor-dialog.component';

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
    MatMenuModule,
    TuiInputModule,
    TuiFieldErrorModule,
    TuiComboBoxModule,
    TuiDataListWrapperModule,
    TuiFilterByInputPipeModule,
    TuiStringifyContentPipeModule,
    TuiTextfieldControllerModule,
    TuiButtonModule
  ],
  exports: [ConnectionEditorComponent, TableImporterComponent,
    DbConnectionWizardComponent,
    TableSelectorComponent],
  declarations: [ConnectionEditorComponent,
    TableSelectorComponent,
    TableImporterComponent,
    TypeSelectorCellEditorComponent,
    CheckboxCellEditorComponent,
    DynamicFormComponentComponent,
    DbConnectionWizardComponent,
    TableSelectorContainerComponent,
    TableImporterContainerComponent,
    DbConnectionEditorDialogComponent,
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

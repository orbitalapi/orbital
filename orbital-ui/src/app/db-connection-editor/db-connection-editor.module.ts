import {NgModule} from '@angular/core';

import {ConnectionEditorComponent} from './connection-editor.component';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {TableSelectorComponent} from './table-selector.component';
import {TableImporterComponent} from './table-importer.component';
import {TypeSelectorCellEditorComponent} from './type-selector-cell-editor.component';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {CheckboxCellEditorComponent} from './checkbox-cell-editor.component';
import {DynamicFormComponentComponent} from './dynamic-form-component.component';
import {DbConnectionWizardComponent} from './db-connection-wizard.component';
import {MatStepperModule} from '@angular/material/stepper';
import {RouterModule} from '@angular/router';
import {TableSelectorContainerComponent} from './table-selector-container.component';
import {TableImporterContainerComponent} from './table-importer-container.component';
import {TypedEditorModule} from '../type-editor/type-editor.module';
import {} from '@angular/material/legacy-form-field';
import {
  TuiCheckboxLabeledModule,
  TuiComboBoxModule,
  TuiDataListWrapperModule,
  TuiFieldErrorPipeModule,
  TuiFilterByInputPipeModule,
  TuiInputModule,
  TuiInputNumberModule,
  TuiInputPasswordModule, TuiProgressModule,
  TuiStringifyContentPipeModule
} from '@taiga-ui/kit';
import {TuiButtonModule, TuiErrorModule, TuiTextfieldControllerModule} from '@taiga-ui/core';
import {DbConnectionEditorDialogComponent} from './db-connection-editor-dialog.component';
import {HeaderComponentLayoutModule} from 'src/app/header-component-layout/header-component-layout.module';
import {ProjectSelectorModule} from "../project-selector/project-selector.module";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatProgressBarModule} from "@angular/material/progress-bar";
import {AgGridModule} from "ag-grid-angular";

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    TypeAutocompleteModule,
    ReactiveFormsModule,
    TypedEditorModule,
    RouterModule,
    TuiInputModule,
    TuiFieldErrorPipeModule,
    TuiComboBoxModule,
    TuiDataListWrapperModule,
    TuiFilterByInputPipeModule,
    TuiStringifyContentPipeModule,
    TuiTextfieldControllerModule,
    TuiButtonModule,
    HeaderComponentLayoutModule,
    ProjectSelectorModule,
    TuiInputPasswordModule,
    TuiInputNumberModule,
    TuiCheckboxLabeledModule,
    TuiErrorModule,
    TuiProgressModule,
    MatFormFieldModule,
    MatProgressBarModule,
    AgGridModule
  ],
  exports: [
    ConnectionEditorComponent,
    TableImporterComponent,
    DbConnectionWizardComponent,
    TableSelectorComponent],
  declarations: [
    ConnectionEditorComponent,
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
})
export class DbConnectionEditorModule {
}

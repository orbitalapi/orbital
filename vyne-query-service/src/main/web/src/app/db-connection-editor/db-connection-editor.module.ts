import {NgModule} from '@angular/core';

import {ConnectionEditorComponent} from './connection-editor.component';
import {CommonModule} from '@angular/common';
import {MatLegacyCardModule as MatCardModule} from '@angular/material/legacy-card';
import {MatLegacySelectModule as MatSelectModule} from '@angular/material/legacy-select';
import {MatLegacyInputModule as MatInputModule} from '@angular/material/legacy-input';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';
import {TableSelectorComponent} from './table-selector.component';
import {TableImporterComponent} from './table-importer.component';
import {AgGridModule} from 'ag-grid-angular';
import {TypeSelectorCellEditorComponent} from './type-selector-cell-editor.component';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {CheckboxCellEditorComponent} from './checkbox-cell-editor.component';
import {MatLegacyCheckboxModule as MatCheckboxModule} from '@angular/material/legacy-checkbox';
import {CovalentDynamicFormsModule} from '@covalent/dynamic-forms';
import {DynamicFormComponentComponent} from './dynamic-form-component.component';
import {DbConnectionWizardComponent} from './db-connection-wizard.component';
import {MatStepperModule} from '@angular/material/stepper';
import {MatLegacyProgressBarModule as MatProgressBarModule} from '@angular/material/legacy-progress-bar';
import {RouterModule} from '@angular/router';
import {TableSelectorContainerComponent} from './table-selector-container.component';
import {MatLegacyMenuModule as MatMenuModule} from '@angular/material/legacy-menu';
import {TableImporterContainerComponent} from './table-importer-container.component';
import {MatLegacyDialogModule as MatDialogModule} from '@angular/material/legacy-dialog';
import {TypedEditorModule} from '../type-editor/type-editor.module';
import {MAT_LEGACY_FORM_FIELD_DEFAULT_OPTIONS as MAT_FORM_FIELD_DEFAULT_OPTIONS, MatLegacyFormFieldDefaultOptions as MatFormFieldDefaultOptions} from '@angular/material/legacy-form-field';
import {
  TuiComboBoxModule,
  TuiDataListWrapperModule,
  TuiFieldErrorPipeModule,
  TuiFilterByInputPipeModule,
  TuiInputModule,
  TuiStringifyContentPipeModule
} from '@taiga-ui/kit';
import {TuiButtonModule, TuiTextfieldControllerModule} from '@taiga-ui/core';
import {DbConnectionEditorDialogComponent} from './db-connection-editor-dialog.component';
import {HeaderComponentLayoutModule} from 'src/app/header-component-layout/header-component-layout.module';

const appearance: MatFormFieldDefaultOptions = {
  appearance: 'outline'
};

@NgModule({
    imports: [
        CommonModule,
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
      TuiFieldErrorPipeModule,
      TuiComboBoxModule,
      TuiDataListWrapperModule,
      TuiFilterByInputPipeModule,
      TuiStringifyContentPipeModule,
      TuiTextfieldControllerModule,
      TuiButtonModule,
      HeaderComponentLayoutModule
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

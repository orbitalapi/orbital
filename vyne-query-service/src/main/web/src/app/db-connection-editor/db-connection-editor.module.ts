import {NgModule} from '@angular/core';

import {DbConnectionEditorComponent} from './db-connection-editor.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatCardModule} from '@angular/material/card';
import {MatSelectModule} from '@angular/material/select';
import {MatInputModule} from '@angular/material/input';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {TableSelectorComponent} from './table-selector.component';
import {TableImporterComponent} from './table-importer.component';
import {AgGridAngular, AgGridModule} from 'ag-grid-angular';
import {TypeSelectorCellEditorComponent} from './type-selector-cell-editor.component';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {CheckboxCellEditorComponent} from './checkbox-cell-editor.component';
import {MatCheckboxModule} from '@angular/material/checkbox';

@NgModule({
  imports: [CommonModule, BrowserModule, BrowserAnimationsModule,
    MatCardModule, MatSelectModule, MatInputModule, FormsModule, MatButtonModule,
    AgGridModule.withComponents([TypeSelectorCellEditorComponent, CheckboxCellEditorComponent]), TypeAutocompleteModule, MatCheckboxModule],
  exports: [DbConnectionEditorComponent, TableImporterComponent],
  declarations: [DbConnectionEditorComponent, TableSelectorComponent, TableImporterComponent, TypeSelectorCellEditorComponent, CheckboxCellEditorComponent],
  providers: [],
})
export class DbConnectionEditorModule {
}

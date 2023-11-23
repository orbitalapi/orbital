import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {DataWorkbookContainerComponent} from './data-workbook-container.component';
import {DataWorkbookComponent} from './data-workbook.component';
import {DataSourcePanelComponent} from './data-source-panel/data-source-panel.component';
import {DataExplorerModule} from '../data-explorer/data-explorer.module';
import { DataSourceDisplayComponent } from './data-source-panel/data-source-display.component';
import {MatExpansionModule} from '@angular/material/expansion';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatIconModule} from '@angular/material/icon';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';
import { SchemaSelectorComponent } from './schema-selector/schema-selector.component';
import {MatLegacyRadioModule as MatRadioModule} from '@angular/material/legacy-radio';
import {FormsModule} from '@angular/forms';
import {SimpleCodeViewerModule} from '../simple-code-viewer/simple-code-viewer.module';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {CodeEditorModule} from '../code-editor/code-editor.module';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {MatLegacyAutocompleteModule as MatAutocompleteModule} from '@angular/material/legacy-autocomplete';
import {MatLegacySelectModule as MatSelectModule} from '@angular/material/legacy-select';
import {ObjectViewModule} from '../object-view/object-view.module';
import {MatLegacyCheckboxModule as MatCheckboxModule} from '@angular/material/legacy-checkbox';
import {TabbedResultsViewModule} from '../tabbed-results-view/tabbed-results-view.module';
import {MatSidenavModule} from '@angular/material/sidenav';
import {TypedInstancePanelModule} from '../typed-instance-panel/typed-instance-panel.module';
import { SourceModeSelectorComponent } from './source-mode-selector.component';
import { TableEditorComponent } from './table-editor.component';
import {AgGridModule} from 'ag-grid-angular';
import {MatLegacyProgressBarModule as MatProgressBarModule} from '@angular/material/legacy-progress-bar';


@NgModule({
  declarations: [
    DataWorkbookContainerComponent,
    DataWorkbookComponent,
    DataSourcePanelComponent,
    DataSourceDisplayComponent,
    SchemaSelectorComponent,
    SourceModeSelectorComponent,
    TableEditorComponent,
  ],
  exports: [
    DataWorkbookComponent,
    DataSourcePanelComponent,
    DataWorkbookContainerComponent,
    SchemaSelectorComponent,
    SourceModeSelectorComponent
  ],
  imports: [
    CommonModule,
    DataExplorerModule,
    MatExpansionModule,
    MatButtonToggleModule,
    MatIconModule,
    MatButtonModule,
    MatRadioModule,
    FormsModule,
    SimpleCodeViewerModule,
    TypeAutocompleteModule,
    CodeEditorModule,
    HeaderBarModule,
    MatAutocompleteModule,
    MatSelectModule,
    ObjectViewModule,
    MatCheckboxModule,
    TabbedResultsViewModule,
    MatSidenavModule,
    TypedInstancePanelModule,
    AgGridModule,
    MatProgressBarModule
  ]
})
export class DataWorkbookModule {
}

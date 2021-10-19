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
import {MatButtonModule} from '@angular/material/button';
import { SchemaSelectorComponent } from './schema-selector/schema-selector.component';
import {MatRadioModule} from '@angular/material/radio';
import {FormsModule} from '@angular/forms';
import {SimpleCodeViewerModule} from '../simple-code-viewer/simple-code-viewer.module';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {CodeEditorModule} from '../code-editor/code-editor.module';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {MatSelectModule} from '@angular/material/select';
import {ObjectViewModule} from '../object-view/object-view.module';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {TabbedResultsViewModule} from '../tabbed-results-view/tabbed-results-view.module';
import {MatSidenavModule} from '@angular/material/sidenav';
import {TypedInstancePanelModule} from '../typed-instance-panel/typed-instance-panel.module';
import { SourceModeSelectorComponent } from './source-mode-selector.component';
import { TableEditorComponent } from './table-editor.component';
import {AgGridModule} from 'ag-grid-angular';
import {MatProgressBarModule} from '@angular/material/progress-bar';


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

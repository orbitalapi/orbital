import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QueryPanelComponent } from './query-panel.component';
import { SearchModule } from '../search/search.module';
import { TypeAutocompleteModule } from '../type-autocomplete/type-autocomplete.module';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { VyneQueryViewerComponent } from './taxi-viewer/vyne-query-viewer/vyne-query-viewer.component';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { CovalentDynamicFormsModule } from '@covalent/dynamic-forms';
import { ObjectViewModule } from '../object-view/object-view.module';
import { CovalentHighlightModule } from '@covalent/highlight';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { TypedInstancePanelModule } from '../typed-instance-panel/typed-instance-panel.module';
import { QueryWizardComponent } from './query-wizard/query-wizard.component';
import { QueryEditorComponent } from './query-editor/query-editor.component';
import { MatTabsModule } from '@angular/material/tabs';
import { CodeViewerModule } from '../code-viewer/code-viewer.module';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BottomBarComponent } from './query-editor/bottom-bar.component';
import { CounterTimerComponent } from './query-editor/counter-timer.component';
import { CallExplorerModule } from './taxi-viewer/call-explorer/call-explorer.module';

import { AngularSplitModule } from 'angular-split';
import { ErrorPanelComponent } from './error-panel/error-panel.component';
import { HeaderBarModule } from '../header-bar/header-bar.module';
import { MatDialogModule } from '@angular/material/dialog';
import { CodeEditorModule } from '../code-editor/code-editor.module';
import { ResultsTableModule } from '../results-table/results-table.module';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { QueryBuilderComponent } from './query-wizard/query-builder.component';
import { TabbedResultsViewModule } from '../tabbed-results-view/tabbed-results-view.module';
import { RouterModule } from '@angular/router';
import { MatSortModule } from '@angular/material/sort';
import { CovalentFileModule } from '@covalent/core/file';
import { ExpandingPanelSetModule } from '../expanding-panelset/expanding-panel-set.module';
import {
  TuiButtonModule,
  TuiDialogModule, TuiErrorModule,
  TuiHintModule,
  TuiNotificationModule,
  TuiTextfieldControllerModule
} from '@taiga-ui/core';
import { ResultsDownloadModule } from 'src/app/results-download/results-download.module';
import { MatMenuModule } from '@angular/material/menu';
import { ClipboardModule } from '@angular/cdk/clipboard';
import { QuerySnippetPanelModule } from 'src/app/query-snippet-panel/query-snippet-panel.module';
import {
  TuiDataListWrapperModule, TuiFieldErrorPipeModule,
  TuiFilterByInputPipeModule, TuiInputModule,
  TuiSelectModule, TuiStringifyContentPipeModule,
  TuiTabsModule,
  TuiTextAreaModule
} from '@taiga-ui/kit';
import {DataCatalogModule} from "../data-catalog/data-catalog.module";
import {CatalogExplorerPanelModule} from "../catalog-explorer-panel/catalog-explorer-panel.module";
import {SaveQueryPanelComponent} from "./query-editor/save-query-panel.component";
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {AppModule} from "../app.module";
import {DisableControlModule} from "../disable-control/disable-control.module";

@NgModule({
  imports: [
    CommonModule,
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
    MatSidenavModule,
    TypedInstancePanelModule,
    MatTabsModule,
    CodeViewerModule,
    MatProgressSpinnerModule,
    CallExplorerModule,
    AngularSplitModule.forChild(),
    HeaderBarModule,
    MatDialogModule,
    ResultsTableModule,
    MatProgressBarModule,
    CodeEditorModule,
    TabbedResultsViewModule,
    RouterModule,
    MatSortModule,
    ExpandingPanelSetModule,
    TuiButtonModule,
    ResultsDownloadModule,
    MatMenuModule,
    ClipboardModule,
    TuiHintModule,
    QuerySnippetPanelModule,
    TuiTabsModule,
    TuiTextAreaModule,
    TuiSelectModule,
    TuiDataListWrapperModule,
    TuiTextfieldControllerModule,
    CatalogExplorerPanelModule,
    HeaderComponentLayoutModule,
    TuiNotificationModule,
    TuiFilterByInputPipeModule,
    TuiStringifyContentPipeModule,
    TuiInputModule,
    TuiErrorModule,
    TuiFieldErrorPipeModule,
    DisableControlModule,
  ],
  exports: [QueryPanelComponent, QueryEditorComponent, BottomBarComponent,
    ErrorPanelComponent, SaveQueryPanelComponent],
  declarations: [QueryPanelComponent, QueryWizardComponent,
    VyneQueryViewerComponent, QueryEditorComponent, BottomBarComponent, CounterTimerComponent,

    ErrorPanelComponent,
    QueryBuilderComponent,
    SaveQueryPanelComponent

  ],
  providers: [],
})
export class QueryPanelModule {
}

import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {QueryPanelComponent} from './query-panel.component';
import {SearchModule} from '../search/search.module';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {MatLegacyCheckboxModule as MatCheckboxModule} from '@angular/material/legacy-checkbox';
import {MatLegacyFormFieldModule as MatFormFieldModule} from '@angular/material/legacy-form-field';
import {MatLegacySelectModule as MatSelectModule} from '@angular/material/legacy-select';
import {MatLegacyCardModule as MatCardModule} from '@angular/material/legacy-card';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {VyneQueryViewerComponent} from './taxi-viewer/vyne-query-viewer/vyne-query-viewer.component';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatIconModule} from '@angular/material/icon';
import {CovalentDynamicFormsModule} from '@covalent/dynamic-forms';
import {ObjectViewModule} from '../object-view/object-view.module';
import {CovalentHighlightModule} from '@covalent/highlight';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';
import {MatSidenavModule} from '@angular/material/sidenav';
import {TypedInstancePanelModule} from '../typed-instance-panel/typed-instance-panel.module';
import {QueryWizardComponent} from './query-wizard/query-wizard.component';
import {QueryEditorComponent} from './query-editor/query-editor.component';
import {MatLegacyTabsModule as MatTabsModule} from '@angular/material/legacy-tabs';
import {CodeViewerModule} from '../code-viewer/code-viewer.module';
import {MatLegacyProgressSpinnerModule as MatProgressSpinnerModule} from '@angular/material/legacy-progress-spinner';
import {QueryEditorToolbar} from './query-editor/query-editor-toolbar.component';
import {CounterTimerComponent} from './query-editor/counter-timer.component';
import {CallExplorerModule} from './taxi-viewer/call-explorer/call-explorer.module';

import {AngularSplitModule} from 'angular-split';
import {ErrorPanelComponent} from './error-panel/error-panel.component';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {MatLegacyDialogModule as MatDialogModule} from '@angular/material/legacy-dialog';
import {CodeEditorModule} from '../code-editor/code-editor.module';
import {ResultsTableModule} from '../results-table/results-table.module';
import {MatLegacyProgressBarModule as MatProgressBarModule} from '@angular/material/legacy-progress-bar';
import {QueryBuilderComponent} from './query-wizard/query-builder.component';
import {TabbedResultsViewModule} from '../tabbed-results-view/tabbed-results-view.module';
import {RouterModule} from '@angular/router';
import {MatSortModule} from '@angular/material/sort';
import {CovalentFileModule} from '@covalent/core/file';
import {ExpandingPanelSetModule} from '../expanding-panelset/expanding-panel-set.module';
import {
    TuiButtonModule, TuiDataListModule, TuiDropdownModule,
    TuiErrorModule,
    TuiHintModule, TuiHostedDropdownModule,
    TuiNotificationModule,
    TuiTextfieldControllerModule
} from '@taiga-ui/core';
import {ResultsDownloadModule} from 'src/app/results-download/results-download.module';
import {MatLegacyMenuModule as MatMenuModule} from '@angular/material/legacy-menu';
import {ClipboardModule} from '@angular/cdk/clipboard';
import {QuerySnippetPanelModule} from 'src/app/query-snippet-panel/query-snippet-panel.module';
import {
  TuiDataListWrapperModule,
  TuiFieldErrorPipeModule,
  TuiFilterByInputPipeModule,
  TuiInputModule,
  TuiSelectModule,
  TuiStringifyContentPipeModule,
  TuiTabsModule,
  TuiTextAreaModule
} from '@taiga-ui/kit';
import {CatalogExplorerPanelModule} from "../catalog-explorer-panel/catalog-explorer-panel.module";
import {SaveQueryPanelComponent} from "./query-editor/save-query-panel.component";
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {DisableControlModule} from "../disable-control/disable-control.module";
import {QueryHistoryPanelModule} from "../query-history-panel/query-history-panel.module";
import {SavedQueriesPanelModule} from "../saved-queries-panel/saved-queries-panel.module";
import {HttpEndpointPanelComponent} from "./query-editor/http-endpoint-panel.component";
import {ProjectSelectorModule} from "../project-selector/project-selector.module";
import {TuiActiveZoneModule} from "@taiga-ui/cdk";

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
        AngularSplitModule,
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
        QueryHistoryPanelModule,
        SavedQueriesPanelModule,
        ProjectSelectorModule,
        TuiDropdownModule,
        TuiActiveZoneModule,
        TuiHostedDropdownModule,
        TuiDataListModule,

    ],
  exports: [QueryPanelComponent, QueryEditorComponent, QueryEditorToolbar,
    ErrorPanelComponent, SaveQueryPanelComponent],
  declarations: [QueryPanelComponent, QueryWizardComponent,
    VyneQueryViewerComponent, QueryEditorComponent, QueryEditorToolbar, CounterTimerComponent,
    HttpEndpointPanelComponent,
    ErrorPanelComponent,
    QueryBuilderComponent,
    SaveQueryPanelComponent

  ],
  providers: [],
})
export class QueryPanelModule {
}

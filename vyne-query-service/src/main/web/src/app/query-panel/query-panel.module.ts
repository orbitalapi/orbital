import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {QueryPanelComponent} from './query-panel.component';
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
import {ObjectViewModule} from '../object-view/object-view.module';
import {CovalentHighlightModule} from '@covalent/highlight';
import {MatButtonModule} from '@angular/material/button';
import {MatSidenavModule} from '@angular/material/sidenav';
import {TypedInstancePanelModule} from '../typed-instance-panel/typed-instance-panel.module';
import {QueryWizardComponent} from './query-wizard/query-wizard.component';
import {QueryEditorComponent} from './query-editor/query-editor.component';
import {MatTabsModule} from '@angular/material/tabs';
import {CodeViewerModule} from '../code-viewer/code-viewer.module';
import {MonacoEditorModule} from '@materia-ui/ngx-monaco-editor';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {BottomBarComponent} from './query-editor/bottom-bar.component';
import {CounterTimerComponent} from './query-editor/counter-timer.component';
import {CallExplorerModule} from './taxi-viewer/call-explorer/call-explorer.module';

import {AngularSplitModule} from 'angular-split';
import {ErrorPanelComponent} from './error-panel/error-panel.component';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {MatDialogModule} from '@angular/material/dialog';
import {CodeEditorModule} from '../code-editor/code-editor.module';
import {ResultsTableModule} from '../results-table/results-table.module';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {QueryBuilderComponent} from './query-wizard/query-builder.component';
import {TabbedResultsViewModule} from '../tabbed-results-view/tabbed-results-view.module';
import {RouterModule} from '@angular/router';
import {MatSortModule} from '@angular/material/sort';
import {CovalentFileModule} from '@covalent/core/file';
import {ExpandingPanelSetModule} from '../expanding-panelset/expanding-panel-set.module';
import {TuiButtonModule} from '@taiga-ui/core';

@NgModule({
  imports: [
    BrowserModule,
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
    MonacoEditorModule,
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
  ],
  exports: [QueryPanelComponent, QueryEditorComponent, BottomBarComponent,
    ErrorPanelComponent],
  declarations: [QueryPanelComponent, QueryWizardComponent,
    VyneQueryViewerComponent, QueryEditorComponent, BottomBarComponent, CounterTimerComponent,

    ErrorPanelComponent,
    QueryBuilderComponent,
  ],
  providers: [],
})
export class QueryPanelModule {
}

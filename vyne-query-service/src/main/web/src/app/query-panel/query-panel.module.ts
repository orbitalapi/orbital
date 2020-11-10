import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {QueryPanelComponent} from './query-panel.component';
import {FileFactSelectorComponent} from './file-fact-selector/file-fact-selector.component';
import {ResultViewerModule} from './result-display/result-viewer.module';
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
import {CovalentFileModule} from '@covalent/core';
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
import {TabbedResultsViewComponent} from './query-editor/tabbed-results-view.component';
import {CallExplorerModule} from './taxi-viewer/call-explorer/call-explorer.module';
import {AngularSplitModule} from 'angular-split';

@NgModule({
  imports: [
    BrowserModule,
    CommonModule,
    ResultViewerModule,
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
    AngularSplitModule.forChild()
  ],
  exports: [QueryPanelComponent, QueryEditorComponent, BottomBarComponent, TabbedResultsViewComponent],
  declarations: [QueryPanelComponent, QueryWizardComponent, FileFactSelectorComponent,
    VyneQueryViewerComponent, QueryEditorComponent, BottomBarComponent, CounterTimerComponent,
    TabbedResultsViewComponent],
  providers: [],
})
export class QueryPanelModule {
}

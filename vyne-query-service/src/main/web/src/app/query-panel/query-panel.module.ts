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
import { ObjectViewModule } from '../object-view/object-view.module';
import { CovalentHighlightModule } from '@covalent/highlight';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { TypedInstancePanelModule } from '../typed-instance-panel/typed-instance-panel.module';
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
import { TabbedResultsViewModule } from '../tabbed-results-view/tabbed-results-view.module';
import { RouterModule } from '@angular/router';
import { MatSortModule } from '@angular/material/sort';
import { CovalentFileModule } from '@covalent/core/file';
import { ExpandingPanelSetModule } from '../expanding-panelset/expanding-panel-set.module';
import { TuiButtonModule } from '@taiga-ui/core';

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
  ],
  exports: [QueryPanelComponent, QueryEditorComponent, BottomBarComponent,
    ErrorPanelComponent],
  declarations: [QueryPanelComponent,
    VyneQueryViewerComponent, QueryEditorComponent, BottomBarComponent, CounterTimerComponent,

    ErrorPanelComponent,
  ],
  providers: [],
})
export class QueryPanelModule {
}

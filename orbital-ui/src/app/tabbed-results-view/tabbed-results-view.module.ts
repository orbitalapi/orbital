import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {TuiSegmentedModule} from '@taiga-ui/experimental';
import {TabbedResultsViewComponent} from './tabbed-results-view.component';
import {MatTabsModule} from '@angular/material/tabs';
import {ObjectViewModule} from '../object-view/object-view.module';
import {CallExplorerModule} from '../query-panel/taxi-viewer/call-explorer/call-explorer.module';
import {CommonModule} from '@angular/common';
import {LineageDisplayModule} from '../lineage-display/lineage-display.module';
import {ExpandingPanelSetModule} from '../expanding-panelset/expanding-panel-set.module';
import {TuiBadgeModule, TuiProgressModule, TuiTabsModule} from '@taiga-ui/kit';
import {
  TuiButtonModule,
  TuiDataListModule,
  TuiHintModule,
  TuiHostedDropdownModule,
  TuiNotificationModule,
  TuiSvgModule
} from '@taiga-ui/core';
import {JsonViewerModule} from 'src/app/json-viewer/json-viewer.module';
import {QueryErrorsListComponent} from "../query-errors-list/query-errors-list.component";

@NgModule({
  imports: [
    MatTabsModule,
    ObjectViewModule,
    FormsModule,
    CommonModule,
    CallExplorerModule,
    LineageDisplayModule,
    ExpandingPanelSetModule,
    TuiTabsModule,
    TuiHostedDropdownModule,
    TuiButtonModule,
    TuiSvgModule,
    TuiDataListModule,
    JsonViewerModule,
    TuiProgressModule,
    QueryErrorsListComponent,
    TuiBadgeModule,
    TuiHintModule,
    TuiNotificationModule,
    TuiSegmentedModule,
  ],
  exports: [TabbedResultsViewComponent],
  declarations: [TabbedResultsViewComponent],
  providers: []
})
export class TabbedResultsViewModule {
}

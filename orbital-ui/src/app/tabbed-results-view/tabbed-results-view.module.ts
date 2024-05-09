import {NgModule} from '@angular/core';
import { FormsModule } from '@angular/forms';
import {TabbedResultsViewComponent} from './tabbed-results-view.component';
import {MatLegacyTabsModule as MatTabsModule} from '@angular/material/legacy-tabs';
import {ObjectViewModule} from '../object-view/object-view.module';
import {CallExplorerModule} from '../query-panel/taxi-viewer/call-explorer/call-explorer.module';
import {CommonModule} from '@angular/common';
import {LineageDisplayModule} from '../lineage-display/lineage-display.module';
import {ExpandingPanelSetModule} from '../expanding-panelset/expanding-panel-set.module';
import { TuiBadgeModule, TuiCheckboxLabeledModule, TuiProgressModule, TuiTabsModule } from '@taiga-ui/kit';
import {
  TuiButtonModule,
  TuiDataListModule,
  TuiHintModule,
  TuiHostedDropdownModule,
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
    TuiCheckboxLabeledModule,
    TuiHintModule

  ],
  exports: [TabbedResultsViewComponent],
  declarations: [TabbedResultsViewComponent],
  providers: []
})
export class TabbedResultsViewModule {
}

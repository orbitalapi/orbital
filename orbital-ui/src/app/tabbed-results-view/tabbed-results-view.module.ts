import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {DropdownComponent} from '../query-panel/query-editor/query-editor-toolbar/dropdown/dropdown.component';
import {TabbedResultsViewComponent} from './tabbed-results-view.component';
import {MatTabsModule} from '@angular/material/tabs';
import {ObjectViewModule} from '../object-view/object-view.module';
import {CallExplorerModule} from '../query-panel/taxi-viewer/call-explorer/call-explorer.module';
import {CommonModule} from '@angular/common';
import {LineageDisplayModule} from '../lineage-display/lineage-display.module';
import {ExpandingPanelSetModule} from '../expanding-panelset/expanding-panel-set.module';
import { TuiBadge, TuiTabs, TuiSegmented, TuiProgress } from '@taiga-ui/kit';
import { TuiNotification, TuiDataList, TuiDropdown, TuiIcon, TuiButton, TuiHint } from '@taiga-ui/core';
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
    ...TuiTabs,
    ...TuiDropdown,
    TuiButton,
    TuiIcon,
    ...TuiDataList,
    JsonViewerModule,
    ...TuiProgress,
    QueryErrorsListComponent,
    TuiBadge,
    ...TuiHint,
    TuiNotification,
    TuiSegmented,
    DropdownComponent,
  ],
  exports: [TabbedResultsViewComponent],
  declarations: [TabbedResultsViewComponent],
  providers: []
})
export class TabbedResultsViewModule {
}

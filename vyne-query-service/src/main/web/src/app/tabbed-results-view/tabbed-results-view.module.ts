import {NgModule} from '@angular/core';
import {TabbedResultsViewComponent} from './tabbed-results-view.component';
import {MatTabsModule} from '@angular/material/tabs';
import {ObjectViewModule} from '../object-view/object-view.module';
import {CallExplorerModule} from '../query-panel/taxi-viewer/call-explorer/call-explorer.module';
import {CommonModule} from '@angular/common';

@NgModule({
  imports: [
    MatTabsModule,
    ObjectViewModule,
    CommonModule,
    CallExplorerModule
  ],
  exports: [TabbedResultsViewComponent],
  declarations: [TabbedResultsViewComponent],
  providers: []
})
export class TabbedResultsViewModule {
}

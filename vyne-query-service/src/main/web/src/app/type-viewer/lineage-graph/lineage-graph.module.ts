import {NgModule} from '@angular/core';

import {LineageGraphComponent} from './lineage-graph.component';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {LineageGraphContainerComponent} from './lineage-graph-container.component';

@NgModule({
  imports: [
    NgxGraphModule,
    BrowserModule,
    CommonModule,
    NgxChartsModule
  ],
  exports: [LineageGraphComponent, LineageGraphContainerComponent],
  declarations: [LineageGraphComponent, LineageGraphContainerComponent],
  providers: [],
})
export class LineageGraphModule {
}

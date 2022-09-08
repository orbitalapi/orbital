import {NgModule} from '@angular/core';

import {ServiceLineageGraphComponent} from './service-lineage-graph.component';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {CommonModule} from '@angular/common';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {ServiceLineageGraphContainerComponent} from './service-lineage-graph-container.component';
import {RouterModule} from '@angular/router';

@NgModule({
  imports: [
    NgxGraphModule,
    CommonModule,
    NgxChartsModule,
    RouterModule
  ],
  exports: [ServiceLineageGraphComponent, ServiceLineageGraphContainerComponent],
  declarations: [ServiceLineageGraphComponent, ServiceLineageGraphContainerComponent],
  providers: [],
})
export class LineageGraphModule {
}

import { NgModule } from '@angular/core';

import { ServiceLineageGraphComponent } from './service-lineage-graph.component';
import { NgxGraphModule } from '@swimlane/ngx-graph';
import { CommonModule } from '@angular/common';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { ServiceLineageGraphContainerComponent } from './service-lineage-graph-container.component';
import { RouterModule } from '@angular/router';
import { LineageNodeDiagramComponent } from './lineage-node-diagram/lineage-node-diagram.component';
import { AngularResizeEventModule } from 'angular-resize-event';
import { SchemaDiagramModule } from 'src/app/schema-diagram/schema-diagram.module';

@NgModule({
    imports: [
        NgxGraphModule,
        CommonModule,
        NgxChartsModule,
        AngularResizeEventModule,
        RouterModule,
        SchemaDiagramModule
    ],
  exports: [ServiceLineageGraphComponent, ServiceLineageGraphContainerComponent, LineageNodeDiagramComponent],
  declarations: [ServiceLineageGraphComponent, ServiceLineageGraphContainerComponent, LineageNodeDiagramComponent],
  providers: [],
})
export class LineageGraphModule {
}

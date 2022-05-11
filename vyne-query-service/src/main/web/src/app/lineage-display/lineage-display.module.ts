import {NgModule} from '@angular/core';
import {LineageDisplayComponent} from './lineage-display.component';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {QueryLineageComponent} from './query-lineage.component';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {QueryLineageContainerComponent} from './query-lineage-container.component';
import {MatButtonModule} from '@angular/material/button';
import {MatChipsModule} from '@angular/material/chips';
import {MatIconModule} from '@angular/material/icon';


@NgModule({
  imports: [
    NgxGraphModule,
    // NgxCharts provides tooltips in lineage graph
    NgxChartsModule,
    CommonModule,
    BrowserModule,
    BrowserAnimationsModule,
    MatButtonModule,
    MatChipsModule,
    MatIconModule
  ],
  exports: [LineageDisplayComponent, QueryLineageComponent],
  declarations: [LineageDisplayComponent, QueryLineageComponent, QueryLineageContainerComponent],
  providers: [],
})
export class LineageDisplayModule {
}

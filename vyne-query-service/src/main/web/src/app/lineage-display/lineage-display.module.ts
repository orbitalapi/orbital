import {NgModule} from '@angular/core';
import {LineageDisplayComponent} from './lineage-display.component';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {LineageTimelineComponent} from './lineage-timeline.component';
import {ClarityModule} from '@clr/angular';


@NgModule({
  imports: [
    NgxGraphModule,
    CommonModule,
    BrowserModule,
    BrowserAnimationsModule,
    ClarityModule,
  ],
  exports: [LineageDisplayComponent, LineageTimelineComponent],
  declarations: [LineageDisplayComponent, LineageTimelineComponent],
  providers: [],
})
export class LineageDisplayModule {
}

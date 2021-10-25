import {NgModule} from '@angular/core';
import {LineageDisplayComponent} from './lineage-display.component';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import { QueryLineageComponent } from './query-lineage.component';


@NgModule({
  imports: [
    NgxGraphModule,
    CommonModule,
    BrowserModule,
    BrowserAnimationsModule
  ],
  exports: [LineageDisplayComponent, QueryLineageComponent],
  declarations: [LineageDisplayComponent, QueryLineageComponent],
  providers: [],
})
export class LineageDisplayModule {
}

import {NgModule} from '@angular/core';
import {LineageDisplayComponent} from './lineage-display.component';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';


@NgModule({
  imports: [
    NgxGraphModule,
    CommonModule,
    BrowserModule,
    BrowserAnimationsModule
  ],
  exports: [LineageDisplayComponent],
  declarations: [LineageDisplayComponent],
  providers: [],
})
export class LineageDisplayModule {
}

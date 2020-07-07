import {NgModule} from '@angular/core';
import {ServiceGraphComponent} from './service-graph.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {NgxGraphModule} from '@swimlane/ngx-graph';

@NgModule({
  imports: [CommonModule, BrowserModule, NgxGraphModule],
  exports: [ServiceGraphComponent],
  declarations: [ServiceGraphComponent],
  providers: [],
})
export class ServiceGraphModule {
}

import {NgModule} from '@angular/core';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {TypeLinkGraphComponent} from './type-link-graph.component';
import {TypeLinkGraphContainerComponent} from './type-link-graph-container.component';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {NgxChartsModule} from '@swimlane/ngx-charts';

@NgModule({
  imports: [
    NgxGraphModule,
    BrowserModule,
    CommonModule,
    NgxChartsModule
  ],
  declarations: [
    TypeLinkGraphComponent,
    TypeLinkGraphContainerComponent
  ],
  exports: [
    TypeLinkGraphContainerComponent
  ]
})
export class TypeLinkGraphModule {
}

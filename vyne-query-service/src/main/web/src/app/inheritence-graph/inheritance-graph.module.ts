import {NgModule} from '@angular/core';
import {InheritanceGraphComponent} from './inheritance-graph.component';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouterModule} from '@angular/router';

@NgModule({
  imports: [
    BrowserModule,
    CommonModule,
    NgxGraphModule,
    NgxChartsModule,
    BrowserAnimationsModule,
    RouterModule
  ],
  exports: [InheritanceGraphComponent],
  declarations: [InheritanceGraphComponent],
  providers: [],
})
export class InheritanceGraphModule {

}

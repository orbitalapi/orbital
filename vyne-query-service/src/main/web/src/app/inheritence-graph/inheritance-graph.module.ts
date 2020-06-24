import {NgModule} from '@angular/core';
import {InheritanceGraphComponent} from './inheritance-graph.component';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

@NgModule({
  imports: [
    BrowserModule,
    CommonModule,
    NgxGraphModule,
    BrowserAnimationsModule
  ],
  exports: [InheritanceGraphComponent],
  declarations: [InheritanceGraphComponent],
  providers: [],
})
export class InheritanceGraphModule {

}

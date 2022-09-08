import {NgModule} from '@angular/core';
import {InheritanceGraphComponent} from './inheritance-graph.component';
import {CommonModule} from '@angular/common';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import { NgxChartsModule } from '@swimlane/ngx-charts';

@NgModule({
  imports: [
    CommonModule,
    NgxGraphModule,
    NgxChartsModule,
  ],
  exports: [InheritanceGraphComponent],
  declarations: [InheritanceGraphComponent],
  providers: [],
})
export class InheritanceGraphModule {

}

import {NgModule} from '@angular/core';
import {InheritanceGraphComponent} from './inheritance-graph.component';
import {CommonModule} from '@angular/common';
import {NgxGraphModule} from '@swimlane/ngx-graph';

@NgModule({
  imports: [
    CommonModule,
    NgxGraphModule,
  ],
  exports: [InheritanceGraphComponent],
  declarations: [InheritanceGraphComponent],
  providers: [],
})
export class InheritanceGraphModule {

}

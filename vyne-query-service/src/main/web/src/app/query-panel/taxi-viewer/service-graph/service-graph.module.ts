import {NgModule} from '@angular/core';
import {ServiceGraphComponent} from './service-graph.component';
import {CommonModule} from '@angular/common';
import {NgxGraphModule} from '@swimlane/ngx-graph';

@NgModule({
  imports: [CommonModule, NgxGraphModule],
  exports: [ServiceGraphComponent],
  declarations: [ServiceGraphComponent],
  providers: [],
})
export class ServiceGraphModule {
}

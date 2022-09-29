import { NgModule } from '@angular/core';
import { NgxGraphModule } from '@swimlane/ngx-graph';
import { TypeLinkGraphComponent } from './type-link-graph.component';
import { TypeLinkGraphContainerComponent } from './type-link-graph-container.component';
import { CommonModule } from '@angular/common';

@NgModule({
  imports: [
    NgxGraphModule,
    CommonModule,
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

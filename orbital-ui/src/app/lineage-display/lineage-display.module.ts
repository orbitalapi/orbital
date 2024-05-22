import {NgModule} from '@angular/core';
import {LineageDisplayComponent} from './lineage-display.component';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {CommonModule} from '@angular/common';
import {QueryLineageComponent} from './query-lineage.component';
import {MatButtonModule} from '@angular/material/button';
import {MatChipsModule} from '@angular/material/chips';
import {MatIconModule} from '@angular/material/icon';


@NgModule({
  imports: [
    NgxGraphModule,
    CommonModule,
    MatButtonModule,
    MatChipsModule,
    MatIconModule
  ],
  exports: [LineageDisplayComponent, QueryLineageComponent],
  declarations: [LineageDisplayComponent, QueryLineageComponent],
  providers: [],
})
export class LineageDisplayModule {
}

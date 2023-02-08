import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JsonViewerComponent } from './json-viewer.component';
import { JsonResultsViewComponent } from './json-results-view.component';


@NgModule({
  declarations: [
    JsonViewerComponent,
    JsonResultsViewComponent
  ],
  exports: [
    JsonViewerComponent,
    JsonResultsViewComponent
  ],
  imports: [
    CommonModule
  ]
})
export class JsonViewerModule { }

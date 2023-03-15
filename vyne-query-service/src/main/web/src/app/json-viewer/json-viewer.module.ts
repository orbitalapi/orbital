import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JsonViewerComponent } from './json-viewer.component';
import { JsonResultsViewComponent } from './json-results-view.component';
import { ExpandingPanelSetModule } from 'src/app/expanding-panelset/expanding-panel-set.module';
import { TuiButtonModule } from '@taiga-ui/core';


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
    CommonModule,
    ExpandingPanelSetModule,
    TuiButtonModule
  ]
})
export class JsonViewerModule { }

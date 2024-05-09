import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JsonViewerComponent } from './json-viewer.component';
import { JsonResultsViewComponent } from './json-results-view.component';
import { ExpandingPanelSetModule } from 'src/app/expanding-panelset/expanding-panel-set.module';
import { TuiButtonModule, TuiNotificationModule } from '@taiga-ui/core';
import { TuiCheckboxLabeledModule } from "@taiga-ui/kit";
import { FormsModule } from "@angular/forms";


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
    TuiButtonModule,
    TuiCheckboxLabeledModule,
    FormsModule,
    TuiNotificationModule
  ]
})
export class JsonViewerModule { }

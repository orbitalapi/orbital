import { TuiCheckbox } from "@taiga-ui/kit";
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JsonViewerComponent } from './json-viewer.component';
import { JsonResultsViewComponent } from './json-results-view.component';
import { ExpandingPanelSetModule } from 'src/app/expanding-panelset/expanding-panel-set.module';
import { TuiNotification, TuiLabel, TuiButton } from '@taiga-ui/core';
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
    TuiButton,
    TuiLabel,
    FormsModule,
    TuiNotification,
    TuiCheckbox
]
})
export class JsonViewerModule { }

import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ProjectSelectorComponent} from './project-selector.component';
import {
  TuiDataListWrapperModule,
  TuiFilterByInputPipeModule,
  TuiSelectModule,
  TuiStringifyContentPipeModule
} from "@taiga-ui/kit";
import {FormsModule} from "@angular/forms";
import {DisableControlModule} from "../disable-control/disable-control.module";


@NgModule({
  declarations: [
    ProjectSelectorComponent
  ],
  exports: [
    ProjectSelectorComponent
  ],
  imports: [
    CommonModule,
    TuiSelectModule,
    FormsModule,
    DisableControlModule,
    TuiDataListWrapperModule,
    TuiFilterByInputPipeModule,
    TuiStringifyContentPipeModule
  ]
})
export class ProjectSelectorModule {
}

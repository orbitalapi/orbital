import { TuiSelectModule } from "@taiga-ui/legacy";
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ProjectSelectorComponent} from './project-selector.component';
import { TuiDataListWrapper, TuiStringifyContentPipe, TuiFilterByInputPipe } from "@taiga-ui/kit";
import {FormsModule} from "@angular/forms";
import {DisableControlModule} from "../disable-control/disable-control.module";
import {RouterModule} from "@angular/router";
import { TuiNotification, TuiDropdown } from '@taiga-ui/core';


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
        ...TuiDataListWrapper,
        TuiFilterByInputPipe,
        TuiStringifyContentPipe,
        RouterModule,
        TuiNotification,
        ...TuiDropdown
    ]
})
export class ProjectSelectorModule {
}

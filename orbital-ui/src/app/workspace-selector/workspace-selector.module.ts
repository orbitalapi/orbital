import { TuiTextfieldControllerModule, TuiSelectModule } from "@taiga-ui/legacy";
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {WorkspaceSelectorComponent} from "./workspace-selector.component";
import { TuiDataList, TuiIcon } from "@taiga-ui/core";
import {FormsModule} from "@angular/forms";
import {VyneServicesModule} from "../services/vyne-services.module";
import {RouterModule} from "@angular/router";


@NgModule({
    declarations: [
        WorkspaceSelectorComponent
    ],
    exports: [
        WorkspaceSelectorComponent
    ],
    imports: [
        CommonModule,
        TuiIcon,
        TuiSelectModule,
        ...TuiDataList,
        FormsModule,
        TuiTextfieldControllerModule,
        VyneServicesModule,
        RouterModule
    ]
})
export class WorkspaceSelectorModule {
}

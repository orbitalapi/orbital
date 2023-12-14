import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {WorkspaceSelectorComponent} from "./workspace-selector.component";
import {TuiDataListModule, TuiSvgModule, TuiTextfieldControllerModule} from "@taiga-ui/core";
import {TuiSelectModule} from "@taiga-ui/kit";
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
        TuiSvgModule,
        TuiSelectModule,
        TuiDataListModule,
        FormsModule,
        TuiTextfieldControllerModule,
        VyneServicesModule,
        RouterModule
    ]
})
export class WorkspaceSelectorModule {
}

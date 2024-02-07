import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {NewWorkspaceComponent} from './new-workspace/new-workspace.component';
import {RouterModule} from "@angular/router";
import {AuthGuard} from "../services/auth.guard";
import {VynePrivileges} from "../services/user-info.service";
import {HeaderBarModule} from "../header-bar/header-bar.module";
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {TuiFieldErrorPipeModule, TuiInputModule, TuiTabsModule} from "@taiga-ui/kit";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {TuiButtonModule, TuiErrorModule, TuiNotificationModule} from "@taiga-ui/core";
import { UiCustomisations } from '../../environments/ui-customisations';


@NgModule({
    declarations: [
        NewWorkspaceComponent
    ],
    imports: [
        CommonModule,
        RouterModule.forChild([
            {
                path: 'new',
                component: NewWorkspaceComponent,
                title: `${UiCustomisations.productName}: Workspaces`
                // canActivate: [AuthGuard],
                // data: {requiredAuthority: VynePrivileges.CreateWorkspace}
            }
        ]),
        HeaderBarModule,
        HeaderComponentLayoutModule,
        TuiTabsModule,
        ReactiveFormsModule,
        TuiInputModule,
        FormsModule,
        TuiButtonModule,
        TuiErrorModule,
        TuiFieldErrorPipeModule,
        TuiNotificationModule
    ]
})
export class WorkspaceManagerModule {
}

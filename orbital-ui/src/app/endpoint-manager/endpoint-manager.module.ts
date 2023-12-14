import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {EndpointListComponent} from './endpoint-list.component';
import {RouterModule} from "@angular/router";
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {TuiCheckboxLabeledModule, TuiDataListWrapperModule, TuiSelectModule, TuiTabsModule} from "@taiga-ui/kit";
import { EndpointMonitorComponent } from './endpoint-monitor.component';
import {ExpandingPanelSetModule} from "../expanding-panelset/expanding-panel-set.module";
import {CodeViewerModule} from "../code-viewer/code-viewer.module";
import {NgApexchartsModule} from "ng-apexcharts";
import {FormsModule} from "@angular/forms";
import {TuiDataListModule, TuiTextfieldControllerModule} from "@taiga-ui/core";

@NgModule({
  declarations: [
    EndpointListComponent,
    EndpointMonitorComponent
  ],
    imports: [
        CommonModule,
        RouterModule.forChild([
            {
                path: '',
                component: EndpointListComponent
            },
            {
                path: ':endpointName',
                component: EndpointMonitorComponent
            }
        ]),
        HeaderComponentLayoutModule,
        TuiTabsModule,
        ExpandingPanelSetModule,
        CodeViewerModule,
        NgApexchartsModule,
        TuiSelectModule,
        FormsModule,
        TuiDataListWrapperModule,
        TuiTextfieldControllerModule,
        TuiDataListModule,
        TuiCheckboxLabeledModule,
    ]
})
export class EndpointManagerModule {
}

import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {EndpointListComponent} from './endpoint-list.component';
import {RouterModule} from "@angular/router";
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {TuiTabsModule} from "@taiga-ui/kit";
import { EndpointMonitorComponent } from './endpoint-monitor.component';
import {ExpandingPanelSetModule} from "../expanding-panelset/expanding-panel-set.module";
import {CodeViewerModule} from "../code-viewer/code-viewer.module";
import {TuiAxesModule, TuiLineChartModule} from "@taiga-ui/addon-charts";


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
        TuiAxesModule,
        TuiLineChartModule
    ]
})
export class EndpointManagerModule {
}

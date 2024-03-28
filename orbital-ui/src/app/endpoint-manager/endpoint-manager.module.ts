import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {EndpointListComponent} from './endpoint-list.component';
import {RouterModule} from "@angular/router";
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {
  TuiBadgeModule,
  TuiCheckboxLabeledModule,
  TuiDataListWrapperModule,
  TuiSelectModule,
  TuiTabsModule, TuiToggleModule
} from "@taiga-ui/kit";
import {EndpointMonitorComponent} from './endpoint-monitor.component';
import {ExpandingPanelSetModule} from "../expanding-panelset/expanding-panel-set.module";
import {CodeViewerModule} from "../code-viewer/code-viewer.module";
import {NgApexchartsModule} from "ng-apexcharts";
import {FormsModule} from "@angular/forms";
import {TuiDataListModule, TuiNotificationModule, TuiTextfieldControllerModule} from "@taiga-ui/core";
import {UiCustomisations} from '../../environments/ui-customisations';
import {ConnectionStatusComponent} from "../data-source-manager/connection-status/connection-status.component";

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
        component: EndpointListComponent,
        title: `${UiCustomisations.productName}: Endpoints`
      },
      {
        path: ':endpointName',
        component: EndpointMonitorComponent,
        title: `${UiCustomisations.productName}: Endpoints`
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
    TuiNotificationModule,
    ConnectionStatusComponent,
    TuiBadgeModule,
    TuiToggleModule
  ]
})
export class EndpointManagerModule {
}

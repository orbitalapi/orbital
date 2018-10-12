import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {SharedModule} from "../shared/shared.module";
import {Ng2SearchPipeModule} from "ng2-search-filter";
import {CommonApiModule} from 'app/common-api/common-api.module';
import {NgxChartsDagModule} from '@swimlane/ngx-charts-dag';
import {TooltipModule} from "ngx-tooltip";
import {TypeLinksGraphComponent} from "./type-links-graph.component";


@NgModule({
   declarations: [TypeLinksGraphComponent],
   imports: [
      CommonModule,
      CommonApiModule,
      SharedModule,
      Ng2SearchPipeModule,
      NgxChartsDagModule,
      TooltipModule
   ],
   providers: [],
   exports: [
      TypeLinksGraphComponent
   ]
})
export class TypeGraphModule {
}

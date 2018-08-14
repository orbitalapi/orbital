import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TypeExplorerComponent } from "./type-explorer.component";
import { SharedModule } from "../shared/shared.module";
import { Ng2SearchPipeModule } from "ng2-search-filter";
import { CommonApiModule } from 'app/common-api/common-api.module';
import { Nl2BrPipe } from "nl2br-pipe";
import { NgxChartsDagModule } from '@swimlane/ngx-charts-dag';
import { TooltipModule } from "ngx-tooltip";
import { TypeLinksGraphComponent } from '../type-graph/type-links-graph.component';
import {TypeGraphModule} from "../type-graph/type-graph.module";

const TYPE_EXPLORER_ROUTE = [{ path: '', component: TypeExplorerComponent }];

@NgModule({
   declarations: [TypeExplorerComponent, Nl2BrPipe],
   imports: [
      CommonModule,
      CommonApiModule,
      SharedModule,
      Ng2SearchPipeModule,
      NgxChartsDagModule,
      TooltipModule,
      TypeGraphModule,
      RouterModule.forChild(TYPE_EXPLORER_ROUTE)
   ],
   providers: []
})
export class TypeExplorerModule { }

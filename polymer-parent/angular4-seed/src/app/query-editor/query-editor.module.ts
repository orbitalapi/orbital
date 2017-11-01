import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from "../shared/shared.module";
import { Ng2SearchPipeModule } from "ng2-search-filter";

import { CovalentHighlightModule } from '@covalent/highlight';
import { CovalentMarkdownModule } from '@covalent/markdown';
import { CovalentCodeEditorModule } from '@covalent/code-editor';

import { NgxChartsDagModule } from '@swimlane/ngx-charts-dag';
import { NgxDatatableModule } from "@swimlane/ngx-datatable";
import { QueryEditorComponent } from 'app/query-editor/query-editor.component';
import { CovalentExpansionPanelModule } from '@covalent/core';
import { CommonApiModule } from 'app/common-api/common-api.module';

const QUERY_EDITOR_ROUTE = [{ path: '', component: QueryEditorComponent }];

@NgModule({
   declarations: [QueryEditorComponent],
   imports: [
      CommonModule,
      CommonApiModule,
      SharedModule,
      RouterModule.forChild(QUERY_EDITOR_ROUTE),
      CovalentExpansionPanelModule

   ]
   // providers: [SchemaEditorService]
})
export class QueryEditorModule { }

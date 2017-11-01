import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SchemaEditorComponent } from "./schema-editor.component";
import { SharedModule } from "../shared/shared.module";
import { Ng2SearchPipeModule } from "ng2-search-filter";
import { SchemaEditorService } from './schema-editor.service';

import { CovalentHighlightModule } from '@covalent/highlight';
import { CovalentMarkdownModule } from '@covalent/markdown';
import { CovalentCodeEditorModule } from '@covalent/code-editor';

import { NgxChartsDagModule } from '@swimlane/ngx-charts-dag';
import { NgxDatatableModule } from "@swimlane/ngx-datatable";

const SCHEMA_EDITOR_ROUTE = [{ path: '', component: SchemaEditorComponent }];

@NgModule({
   declarations: [SchemaEditorComponent],
   imports: [
      CommonModule,
      SharedModule,
      Ng2SearchPipeModule,
      RouterModule.forChild(SCHEMA_EDITOR_ROUTE),

      CovalentHighlightModule,
      CovalentMarkdownModule,
      CovalentCodeEditorModule,

      NgxChartsDagModule,
      NgxDatatableModule


   ],
   providers: [SchemaEditorService]
})
export class SchemaEditorModule { }

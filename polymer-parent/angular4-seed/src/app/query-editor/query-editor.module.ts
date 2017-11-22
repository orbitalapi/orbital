import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from "../shared/shared.module";
import { Ng2SearchPipeModule } from "ng2-search-filter";

import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CovalentHighlightModule } from '@covalent/highlight';
import { CovalentMarkdownModule } from '@covalent/markdown';
import { CovalentCodeEditorModule } from '@covalent/code-editor';

import { QueryEditorComponent } from './query-editor.component';
import { CovalentExpansionPanelModule } from '@covalent/core';
import { CommonApiModule } from 'app/common-api/common-api.module';
import { FactEditorComponent } from './fact-editor.component';
import { CovalentDynamicFormsModule } from '@covalent/dynamic-forms';
import { MaterialLabDbService } from 'app/shared/data/MaterialLabDb.service';
import { InMemoryWebApiModule } from 'angular-in-memory-web-api';
import { NavDropDownDirectives } from 'app/shared/directives/nav-dropdown.directive';
import { TreeModule } from 'angular-tree-component';

const QUERY_EDITOR_ROUTE = [{ path: '', component: QueryEditorComponent }];

@NgModule({
   imports: [
      CommonModule,
      CommonApiModule,
      FormsModule, ReactiveFormsModule,

      SharedModule,
      RouterModule.forChild(QUERY_EDITOR_ROUTE),
      CovalentExpansionPanelModule,
      CovalentDynamicFormsModule,

      TreeModule,

      // Note:  SHouldn't be neccessary (and should remove when real
      // backend exists.  But CovalentDynamicFormsModule seems to
      // cause this to break when declared in the app module
      // InMemoryWebApiModule.forRoot(MaterialLabDbService),

   ],
   declarations: [
      QueryEditorComponent,
      FactEditorComponent,
      // NavDropDownDirectives
   ],


   // providers: [SchemaEditorService]
})
export class QueryEditorModule { }

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TypeExplorerComponent } from "./type-explorer.component";
import { SharedModule } from "../shared/shared.module";
import { Ng2SearchPipeModule } from "ng2-search-filter";
import { CommonApiModule } from 'app/common-api/common-api.module';

const TYPE_EXPLORER_ROUTE = [{ path: '', component: TypeExplorerComponent }];

@NgModule({
   declarations: [TypeExplorerComponent],
   imports: [
      CommonModule,
      CommonApiModule,
      SharedModule,
      Ng2SearchPipeModule,
      RouterModule.forChild(TYPE_EXPLORER_ROUTE)
   ],
   providers: []
})
export class TypeExplorerModule { }

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TypeExplorerComponent } from "./type-explorer.component";
import { SharedModule } from "../shared/shared.module";
import { Ng2SearchPipeModule } from "ng2-search-filter";
import { TypeExplorerService } from './type-explorer.service';

const TYPE_EXPLORER_ROUTE = [{ path: '', component: TypeExplorerComponent }];

@NgModule({
   declarations: [TypeExplorerComponent],
   imports: [
      CommonModule,
      SharedModule,
      Ng2SearchPipeModule,
      RouterModule.forChild(TYPE_EXPLORER_ROUTE)
   ],
   providers: [TypeExplorerService]
})
export class TypeExplorerModule { }

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from "../shared/shared.module";
import { Ng2SearchPipeModule } from "ng2-search-filter";
import { TypesService } from './types.service';


@NgModule({
   imports: [
      SharedModule,
   ],
   providers: [TypesService]
})
export class CommonApiModule { }

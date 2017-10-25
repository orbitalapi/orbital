
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { RightSideNavV2Component } from './right-side-nav-v2.component';
import { SharedModule } from '../../../shared/shared.module';


const RightSideNavV2_ROUTE = [
    { path: '', component: RightSideNavV2Component },
];

@NgModule({
	  declarations: [RightSideNavV2Component],
    imports: [
			CommonModule,
			SharedModule,
			RouterModule.forChild(RightSideNavV2_ROUTE)
    ]
  
})
export class RightSideNavV2Module { }


import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LeftSideNavV2Component } from './left-side-nav-v2.component';
import { SharedModule } from '../../../shared/shared.module';


const LeftSideNavV2_ROUTE = [
    { path: '', component: LeftSideNavV2Component },
];

@NgModule({
	  declarations: [LeftSideNavV2Component],
    imports: [
			CommonModule,
			SharedModule,
			RouterModule.forChild(LeftSideNavV2_ROUTE)
    ]
  
})
export class LeftSideNavV2Module { }

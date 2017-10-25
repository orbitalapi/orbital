
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LeftSideNavV1Component } from './left-side-nav-v1.component';
import { SharedModule } from '../../../shared/shared.module';


const LeftSideNavV1_ROUTE = [
    { path: '', component: LeftSideNavV1Component },
];

@NgModule({
	  declarations: [LeftSideNavV1Component],
    imports: [
			CommonModule,
			SharedModule,
			RouterModule.forChild(LeftSideNavV1_ROUTE)
    ]
  
})
export class LeftSideNavV1Module { }

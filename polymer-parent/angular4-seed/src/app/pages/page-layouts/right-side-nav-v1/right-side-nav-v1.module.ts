
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { RightSideNavV1Component } from './right-side-nav-v1.component';
import { SharedModule } from '../../../shared/shared.module';

const RightSideNavV1_ROUTE = [
    { path: '', component: RightSideNavV1Component },
];

@NgModule({
	  declarations: [RightSideNavV1Component],
    imports: [
			CommonModule,
			SharedModule,
			RouterModule.forChild(RightSideNavV1_ROUTE)
    ]
  
})
export class RightSideNavV1Module { }

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FullWidthV1Component } from './full-width-v1.component';
import { SharedModule } from '../../../shared/shared.module';


const FullWidthV1_ROUTE = [
    { path: '', component: FullWidthV1Component },
];

@NgModule({
	  declarations: [FullWidthV1Component],
    imports: [
			CommonModule,
			SharedModule,
			RouterModule.forChild(FullWidthV1_ROUTE)
    ]
  
})
export class FullWidthV1Module { }

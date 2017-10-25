import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FullWidthV2Component } from './full-width-v2.component';
import { SharedModule } from '../../../shared/shared.module';


const FullWidthV2_ROUTE = [
    { path: '', component: FullWidthV2Component },
];

@NgModule({
	  declarations: [FullWidthV2Component],
    imports: [
			CommonModule,
			SharedModule,
			RouterModule.forChild(FullWidthV2_ROUTE)
    ]
  
})
export class FullWidthV2Module { }

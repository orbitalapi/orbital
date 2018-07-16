import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { BoxedV2Component } from './boxed-layout-v2.component';
import { SharedModule } from '../../../shared/shared.module';


const BoxedV2_ROUTE = [
    { path: '', component: BoxedV2Component },
];

@NgModule({
	  declarations: [BoxedV2Component],
    imports: [
			CommonModule,
			SharedModule,
			RouterModule.forChild(BoxedV2_ROUTE)
    ]
  
})
export class BoxedV2Module { }

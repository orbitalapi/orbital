import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { BoxedV1Component } from './boxed-layout-v1.component';
import { SharedModule } from '../../../shared/shared.module';


const BoxedV1_ROUTE = [
    { path: '', component: BoxedV1Component },
];

@NgModule({
	  declarations: [BoxedV1Component],
    imports: [
			CommonModule,
			SharedModule,
			RouterModule.forChild(BoxedV1_ROUTE)
    ]
  
})
export class BoxedV1Module { }

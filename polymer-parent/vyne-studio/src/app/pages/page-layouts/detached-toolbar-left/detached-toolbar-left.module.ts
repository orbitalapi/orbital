import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { DetachedToolbarLeftComponent } from './detached-toolbar-left.component';
import { SharedModule } from '../../../shared/shared.module';


const DETACHED_TOOLBAR_LEFT_ROUTE = [
    { path: '', component: DetachedToolbarLeftComponent },
];

@NgModule({
	  declarations: [DetachedToolbarLeftComponent],
    imports: [
			CommonModule,
			SharedModule,
			RouterModule.forChild(DETACHED_TOOLBAR_LEFT_ROUTE)
    ]
  
})
export class DetachedToolbarLeftModule { }

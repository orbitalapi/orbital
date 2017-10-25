
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { DetachedToolbarRightComponent } from './detached-toolbar-right.component';
import { SharedModule } from '../../../shared/shared.module';


const DETACHED_TOOLBAR_RIGHT_ROUTE = [
    { path: '', component: DetachedToolbarRightComponent },
];

@NgModule({
	  declarations: [DetachedToolbarRightComponent],
    imports: [
			CommonModule,
			SharedModule,
			RouterModule.forChild(DETACHED_TOOLBAR_RIGHT_ROUTE)
    ]
  
})
export class DetachedToolbarRightModule { }

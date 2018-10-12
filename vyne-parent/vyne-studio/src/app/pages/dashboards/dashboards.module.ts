import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { DashboardsComponent } from './dashboards.component';
import { SharedModule } from '../../shared/shared.module';
const DASHBOARDS_ROUTE = [
    { path: '', component: DashboardsComponent },
];

@NgModule({
	  declarations: [
			DashboardsComponent
		],
    imports: [
			CommonModule,
			SharedModule,
			RouterModule.forChild(DASHBOARDS_ROUTE)
    ]
  
})
export class DashboardsModule { }

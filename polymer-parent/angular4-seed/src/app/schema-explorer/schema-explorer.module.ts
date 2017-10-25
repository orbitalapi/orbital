import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import {SchemaExplorerComponent} from "./schema-explorer.component";
import {SharedModule} from "../shared/shared.module";
const DASHBOARDS_ROUTE = [
    { path: '', component: SchemaExplorerComponent },
];

@NgModule({
	  declarations: [
			SchemaExplorerComponent
		],
    imports: [
			CommonModule,
			SharedModule,
			RouterModule.forChild(DASHBOARDS_ROUTE)
    ]

})
export class SchemaExplorerModule { }

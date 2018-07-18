import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';
import {TypeExplorerComponent} from "./type-explorer.component";
// import {SharedModule} from "../shared/shared.module";
import {Ng2SearchPipeModule} from "ng2-search-filter";
// import {CommonApiModule} from 'app/common-api/common-api.module';
import {Nl2BrPipe} from "nl2br-pipe";
import {TypeLinksGraphComponent} from './type-links-graph.component';
import {MatCheckboxModule, MatExpansionModule, MatIconModule, MatMenuModule} from '@angular/material';
import {MatSidenavModule} from '@angular/material/sidenav';
import {MatTabsModule} from '@angular/material/tabs';

import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {HttpClientModule} from "@angular/common/http";
import {NgxGraphModule} from "@swimlane/ngx-graph";
import {FuseSharedModule} from "../../../@fuse/shared.module";
import { TypeListComponent } from './type-list/type-list.component';
import { TypeDetailComponent } from './type-detail/type-detail.component';

const TYPE_EXPLORER_ROUTE = [{path: 'type-explorer', component: TypeExplorerComponent}];

@NgModule({
    declarations: [
        TypeExplorerComponent,
        Nl2BrPipe,
        TypeLinksGraphComponent,
        TypeListComponent,
        TypeDetailComponent
    ],
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        HttpClientModule,
        // CommonApiModule,
        // SharedModule,
        MatExpansionModule,
        MatSidenavModule,
        MatTabsModule,
        MatIconModule,
        MatMenuModule,
        MatCheckboxModule,

        Ng2SearchPipeModule,
        NgxGraphModule,
        RouterModule.forChild(TYPE_EXPLORER_ROUTE),

        FuseSharedModule
    ],
    providers: []
})
export class TypeExplorerModule {
}

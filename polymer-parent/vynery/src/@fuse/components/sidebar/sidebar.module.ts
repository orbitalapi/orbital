import { NgModule } from '@angular/core';

import { FuseSidebarComponent } from './sidebar.component';

@NgModule({
    declarations: [
        FuseSidebarComponent
    ],
    exports     : [
        FuseSidebarComponent
    ]
})
export class FuseSidebarModule
{
}

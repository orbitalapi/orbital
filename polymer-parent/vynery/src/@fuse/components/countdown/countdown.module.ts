import { NgModule } from '@angular/core';

import { FuseCountdownComponent } from '@fuse/components/countdown/countdown.component';

@NgModule({
    declarations: [
        FuseCountdownComponent
    ],
    exports: [
        FuseCountdownComponent
    ],
})
export class FuseCountdownModule
{
}

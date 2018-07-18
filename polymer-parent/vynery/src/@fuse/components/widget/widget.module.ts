import { NgModule } from '@angular/core';

import { FuseWidgetComponent } from './widget.component';
import { FuseWidgetToggleDirective } from './widget-toggle.directive';

@NgModule({
    declarations: [
        FuseWidgetComponent,
        FuseWidgetToggleDirective
    ],
    exports     : [
        FuseWidgetComponent,
        FuseWidgetToggleDirective
    ],
})
export class FuseWidgetModule
{
}

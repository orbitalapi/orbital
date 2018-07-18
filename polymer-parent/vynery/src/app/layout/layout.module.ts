import {NgModule} from '@angular/core';

import {VerticalLayout1Module} from 'app/layout/vertical/layout-1/layout-1.module';

@NgModule({
    imports: [
        VerticalLayout1Module,
        // VerticalLayout2Module,
        // VerticalLayout3Module,

        // HorizontalLayout1Module
    ],
    exports: [
        VerticalLayout1Module,
        // VerticalLayout2Module,
        // VerticalLayout3Module,
        //
        // HorizontalLayout1Module
    ]
})
export class LayoutModule
{
}

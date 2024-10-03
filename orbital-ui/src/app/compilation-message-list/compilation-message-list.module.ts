import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CompilationMessageListComponent} from '../compilation-message-list.component';
import {ExpandingPanelSetModule} from "../expanding-panelset/expanding-panel-set.module";
import {TuiAccordionModule, TuiBadgeModule} from "@taiga-ui/kit";
import {TuiExpandModule} from "@taiga-ui/core";


@NgModule({
    declarations: [
        CompilationMessageListComponent
    ],
    exports: [
        CompilationMessageListComponent
    ],
    imports: [
        CommonModule,
        ExpandingPanelSetModule,
        TuiAccordionModule,
        TuiBadgeModule,
        TuiExpandModule
    ]
})
export class CompilationMessageListModule {
}

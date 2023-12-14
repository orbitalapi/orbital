import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CompilationMessageListComponent} from '../compilation-message-list.component';
import {ExpandingPanelSetModule} from "../expanding-panelset/expanding-panel-set.module";
import {TuiAccordionModule} from "@taiga-ui/kit";


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
        TuiAccordionModule
    ]
})
export class CompilationMessageListModule {
}

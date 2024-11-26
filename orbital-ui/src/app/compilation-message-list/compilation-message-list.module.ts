import {TuiExpand, TuiHintOverflow} from '@taiga-ui/core';
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CompilationMessageListComponent} from '../compilation-message-list.component';
import {ExpandingPanelSetModule} from "../expanding-panelset/expanding-panel-set.module";
import {TuiAccordion, TuiBadge, TuiBadgeNotification} from '@taiga-ui/kit';

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
    ...TuiAccordion,
    TuiBadge,
    ...TuiExpand,
    TuiBadgeNotification,
    TuiHintOverflow,
  ],
})
export class CompilationMessageListModule {
}

import {NgModule} from '@angular/core';

import {PanelHeaderComponent} from './panel-header.component';
import {CommonModule} from '@angular/common';
import { PanelsetComponent } from './panelset.component';
import { PanelComponent } from './panel.component';
import {DialogModule} from "@angular/cdk/dialog";
import {SvgIconComponent} from "../../../../../vyne/orbital-ui/src/app/svg-icon/svg-icon.component";

@NgModule({
    imports: [
        CommonModule,
        DialogModule,
        SvgIconComponent
    ],
    exports: [PanelHeaderComponent, PanelsetComponent, PanelComponent, PanelHeaderComponent],
    declarations: [PanelHeaderComponent, PanelsetComponent, PanelComponent, PanelHeaderComponent],
  providers: [],
})
export class ExpandingPanelSetModule {
}

import {NgModule} from '@angular/core';
import {SvgIconComponent} from '../svg-icon/svg-icon.component';
import {PanelHeaderComponent} from './panel-header.component';
import {CommonModule} from '@angular/common';
import { PanelsetComponent } from './panelset.component';
import { PanelComponent } from './panel.component';
import {DialogModule} from "@angular/cdk/dialog";

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

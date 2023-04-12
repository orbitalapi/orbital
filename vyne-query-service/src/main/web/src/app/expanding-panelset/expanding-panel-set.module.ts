import {NgModule} from '@angular/core';

import {PanelHeaderComponent} from './panel-header.component';
import {CommonModule} from '@angular/common';
import { PanelsetComponent } from './panelset.component';
import { PanelComponent } from './panel.component';
import {DialogModule} from "@angular/cdk-experimental/dialog";

@NgModule({
  imports: [
    CommonModule,
    DialogModule
  ],
  exports: [PanelHeaderComponent, PanelsetComponent, PanelComponent],
  declarations: [PanelHeaderComponent, PanelsetComponent, PanelComponent],
  providers: [],
})
export class ExpandingPanelSetModule {
}

import {NgModule} from '@angular/core';

import {ExpandingPanelSetComponent} from './expanding-panel-set.component';
import { PanelHeaderComponent } from './panel-header.component';
import {CommonModule} from '@angular/common';

@NgModule({
  imports: [
    CommonModule
  ],
  exports: [ExpandingPanelSetComponent, PanelHeaderComponent],
  declarations: [ExpandingPanelSetComponent, PanelHeaderComponent],
  providers: [],
})
export class ExpandingPanelSetModule {
}

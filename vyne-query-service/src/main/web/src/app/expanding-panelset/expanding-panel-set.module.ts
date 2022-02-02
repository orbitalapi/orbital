import {NgModule} from '@angular/core';

import {PanelHeaderComponent} from './panel-header.component';
import {CommonModule} from '@angular/common';

@NgModule({
  imports: [
    CommonModule
  ],
  exports: [PanelHeaderComponent],
  declarations: [PanelHeaderComponent],
  providers: [],
})
export class ExpandingPanelSetModule {
}

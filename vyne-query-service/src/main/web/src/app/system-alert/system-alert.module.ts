import {NgModule} from '@angular/core';

import {SystemAlertComponent} from './system-alert.component';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';

@NgModule({
  imports: [
    CommonModule,
    MatButtonModule
  ],
  exports: [
    SystemAlertComponent
  ],
  declarations: [SystemAlertComponent],
  providers: [],
})
export class SystemAlertModule {
}

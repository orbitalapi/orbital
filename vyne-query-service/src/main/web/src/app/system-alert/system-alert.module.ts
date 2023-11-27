import {NgModule} from '@angular/core';

import {SystemAlertComponent} from './system-alert.component';
import {CommonModule} from '@angular/common';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';

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

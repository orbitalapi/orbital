import {NgModule} from '@angular/core';

import {SystemAlertComponent} from './system-alert.component';
import {CommonModule} from '@angular/common';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';
import {TuiButtonModule} from "@taiga-ui/core";

@NgModule({
    imports: [
        CommonModule,
        MatButtonModule,
        TuiButtonModule
    ],
  exports: [
    SystemAlertComponent
  ],
  declarations: [SystemAlertComponent],
  providers: [],
})
export class SystemAlertModule {
}

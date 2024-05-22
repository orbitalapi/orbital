import {NgModule} from '@angular/core';

import {SystemAlertComponent} from './system-alert.component';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';
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

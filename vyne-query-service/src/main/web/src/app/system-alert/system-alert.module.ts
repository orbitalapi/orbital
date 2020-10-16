import {NgModule} from '@angular/core';

import {SystemAlertComponent} from './system-alert.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {MatButtonModule} from '@angular/material/button';

@NgModule({
  imports: [
    CommonModule,
    BrowserModule,
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

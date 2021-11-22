import {NgModule} from '@angular/core';

import {ButtonComponent} from './button.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';

@NgModule({
  imports: [CommonModule, BrowserModule],
  exports: [ButtonComponent],
  declarations: [ButtonComponent],
  providers: [],
})
export class ButtonModule {
}

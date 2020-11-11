import {NgModule} from '@angular/core';

import {DataTagComponent} from './data-tag.component';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {MatTooltipModule} from '@angular/material/tooltip';

@NgModule({
  imports: [BrowserModule, CommonModule, MatTooltipModule],
  exports: [DataTagComponent],
  declarations: [DataTagComponent],
  providers: [],
})
export class DataTagModule {
}

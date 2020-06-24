import {NgModule} from '@angular/core';
import {ContentsTableComponent} from './contents-table.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';

@NgModule({
  imports: [
    CommonModule,
    BrowserModule
  ],
  exports: [ContentsTableComponent],
  declarations: [ContentsTableComponent],
  providers: [],
})
export class ContentsTableModule {
}

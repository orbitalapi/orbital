import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {ObjectViewComponent} from './object-view.component';

@NgModule({
  imports: [CommonModule, BrowserModule],
  exports: [ObjectViewComponent],
  declarations: [ObjectViewComponent],
  providers: [],
})
export class ObjectViewModule {
}

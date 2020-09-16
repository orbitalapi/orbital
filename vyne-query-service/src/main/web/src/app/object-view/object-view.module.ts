import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {ObjectViewComponent} from './object-view.component';
import {MatButtonModule} from "@angular/material/button";

@NgModule({
    imports: [CommonModule, BrowserModule, MatButtonModule],
  exports: [ObjectViewComponent],
  declarations: [ObjectViewComponent],
  providers: [],
})
export class ObjectViewModule {
}

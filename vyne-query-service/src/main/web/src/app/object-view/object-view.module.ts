import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {ObjectViewComponent} from './object-view.component';
import {MatButtonModule} from '@angular/material/button';
import {MatMenuModule} from '@angular/material/menu';
import {MatRadioModule} from "@angular/material/radio";

@NgModule({
    imports: [CommonModule, BrowserModule, MatButtonModule, MatMenuModule, MatRadioModule],
  exports: [ObjectViewComponent],
  declarations: [ObjectViewComponent],
  providers: [],
})
export class ObjectViewModule {
}

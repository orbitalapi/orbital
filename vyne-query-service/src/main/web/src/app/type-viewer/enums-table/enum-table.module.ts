import {NgModule} from '@angular/core';
import {EnumTableComponent} from './enum-table.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {RouterModule} from '@angular/router';

@NgModule({
  imports: [CommonModule, BrowserModule, RouterModule],
  exports: [EnumTableComponent],
  declarations: [EnumTableComponent],
  providers: [],
})
export class EnumTableModule {
}

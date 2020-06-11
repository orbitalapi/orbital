import {NgModule} from '@angular/core';
import {TypeListComponent} from './type-list.component';
import {MatToolbarModule} from '@angular/material/toolbar';
import {SearchModule} from '../search/search.module';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {MatButtonModule} from '@angular/material/button';

@NgModule({
  imports: [
    MatToolbarModule,
    MatButtonModule,
    SearchModule,
    CommonModule,
    BrowserModule
  ],
  exports: [TypeListComponent],
  declarations: [TypeListComponent],
  providers: [],
})
export class TypeListModule {
}

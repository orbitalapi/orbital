import {NgModule} from '@angular/core';
import {EnumTableComponent} from './enum-table.component';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';

@NgModule({
  imports: [CommonModule, RouterModule],
  exports: [EnumTableComponent],
  declarations: [EnumTableComponent],
  providers: [],
})
export class EnumTableModule {
}

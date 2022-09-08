import {NgModule} from '@angular/core';
import {ContentsTableComponent} from './contents-table.component';
import {CommonModule} from '@angular/common';

@NgModule({
  imports: [
    CommonModule,
  ],
  exports: [ContentsTableComponent],
  declarations: [ContentsTableComponent],
  providers: [],
})
export class ContentsTableModule {
}

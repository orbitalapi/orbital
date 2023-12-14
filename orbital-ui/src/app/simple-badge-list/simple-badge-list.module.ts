import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SimpleBadgeListComponent } from './simple-badge-list.component';

@NgModule({
  declarations: [
    SimpleBadgeListComponent
  ],
  exports: [
    SimpleBadgeListComponent
  ],
  imports: [
    CommonModule,
  ]
})
export class SimpleBadgeListModule { }

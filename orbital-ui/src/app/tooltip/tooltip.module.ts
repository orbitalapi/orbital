import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TooltipDirective } from './tooltip.directive';


@NgModule({
  declarations: [
    TooltipDirective
  ],
  exports: [TooltipDirective],
  imports: [
    CommonModule
  ]
})
export class TooltipModule {
}

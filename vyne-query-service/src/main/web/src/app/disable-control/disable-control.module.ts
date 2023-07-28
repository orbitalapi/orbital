import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {DisableControlDirective} from "./disable-control.directive";



@NgModule({
  declarations: [DisableControlDirective],
  imports: [
    CommonModule
  ],
  exports: [
    DisableControlDirective
  ]
})
export class DisableControlModule { }

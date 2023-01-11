import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PackageListComponent } from './package-list/package-list.component';



@NgModule({
  declarations: [
    PackageListComponent
  ],
  exports: [
    PackageListComponent
  ],
  imports: [
    CommonModule
  ]
})
export class PackageViewerModule { }

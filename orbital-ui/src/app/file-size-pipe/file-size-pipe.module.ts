import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileSizePipe } from './file-size.pipe';


@NgModule({
  declarations: [
    FileSizePipe
  ],
  exports: [
    FileSizePipe
  ],
  imports: [
    CommonModule
  ]
})
export class FileSizePipeModule {
}

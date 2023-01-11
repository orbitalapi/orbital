import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TruncatePipe } from 'src/app/truncate-pipe/truncate.pipe';


@NgModule({
  declarations: [
    TruncatePipe
  ],
  imports: [
    CommonModule
  ],
  exports: [
    TruncatePipe
  ]
})
export class TruncatePipeModule { }

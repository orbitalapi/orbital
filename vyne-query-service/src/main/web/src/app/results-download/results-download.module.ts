import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TestPackModuleModule } from 'src/app/test-pack-module/test-pack-module.module';
import { MatDialogModule } from '@angular/material/dialog';


@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    TestPackModuleModule,
    MatDialogModule
  ],
})
export class ResultsDownloadModule {
}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TestPackModuleModule } from 'src/app/test-pack-module/test-pack-module.module';
import { MatLegacyDialogModule as MatDialogModule } from '@angular/material/legacy-dialog';


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

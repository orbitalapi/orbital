import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import { CaskViewerComponent } from './cask-viewer.component';
import { CaskRowComponent } from './cask-row.component';
import { MatButtonModule, MatToolbarModule, MatTooltipModule, MatCardModule } from '@angular/material';
import { VyneServicesModule } from '../services/vyne-services.module';
import { SearchModule } from '../search/search.module';
import { MatExpansionModule } from '@angular/material/expansion';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CodeViewerModule } from '../code-viewer/code-viewer.module';
import { CaskDetailsComponent } from './cask-details.component';
import {MatDialogModule} from '@angular/material/dialog';
import { CaskConfirmDialogComponent } from './cask-confirm-dialog.component';
import { CaskSourceViewerComponent } from './cask-source-viewer.component';


@NgModule({
  imports: [
    BrowserModule,
    CommonModule,
    MatButtonModule,
    MatCardModule,
    BrowserAnimationsModule,
    MatExpansionModule,
    MatToolbarModule,
    VyneServicesModule,
    SearchModule,
    CodeViewerModule,
    MatTooltipModule,
    MatDialogModule
  ],
  exports: [CaskViewerComponent, CaskRowComponent, CaskDetailsComponent],
  declarations: [CaskViewerComponent, CaskRowComponent, CaskDetailsComponent, CaskConfirmDialogComponent, CaskSourceViewerComponent],
  providers: [],
  entryComponents: [CaskConfirmDialogComponent]
})
export class CaskViewerModule {
}

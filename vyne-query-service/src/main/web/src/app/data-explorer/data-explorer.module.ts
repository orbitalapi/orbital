import {NgModule} from '@angular/core';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {FileDropModule} from 'ngx-file-drop';
import {DataSourceToolbarComponent} from './data-source-toolbar.component';
import {DataSourceUploadComponent} from './data-source-upload.component';
import {DataExplorerComponent} from './data-explorer.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {FormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatButtonModule} from '@angular/material/button';
import { DataSourceConfigComponent } from './data-source-config.component';
import { FileExtensionIconComponent } from './file-extension-icon.component';


@NgModule({
  imports: [
    CommonModule,
    BrowserModule,
    FormsModule,
    BrowserAnimationsModule,
    TypeAutocompleteModule,
    MatButtonModule,
    // Note to future self:  This is named NgxFileDropModule in later versions
    FileDropModule
  ],
  declarations: [
    DataSourceToolbarComponent,
    DataSourceUploadComponent,
    DataSourceConfigComponent,
    FileExtensionIconComponent,
    // Note: Leaving this in the app module, as it requires modularizing a bunch of stuff
    // DataExplorerComponent
  ],
  exports: [
    DataSourceToolbarComponent,
    FileExtensionIconComponent
  ]
})
export class DataExplorerModule {
}

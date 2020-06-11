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
import {DataSourceConfigComponent} from './data-source-config.component';
import {FileExtensionIconComponent} from './file-extension-icon.component';
import {MatMenuModule} from '@angular/material/menu';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatSelectModule} from '@angular/material/select';
import {MatCheckbox, MatCheckboxModule} from '@angular/material/checkbox';
import {CsvViewerComponent} from './csv-viewer.component';
import {MatTableModule} from '@angular/material/table';
import {MatInputModule} from '@angular/material/input';
import {TypedInstancePanelComponent} from './typed-instance-panel.component';
import {MatSidenavModule} from '@angular/material/sidenav';
import {DescriptionEditorModule} from '../type-viewer/description-editor/description-editor.module';
import {AttributeTableModule} from '../type-viewer/attribute-table/attribute-table.module';
import {EnumTableModule} from '../type-viewer/enums-table/enum-table.module';


@NgModule({
  imports: [
    AttributeTableModule,
    EnumTableModule,

    CommonModule,
    BrowserModule,
    FormsModule,
    BrowserAnimationsModule,
    TypeAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatMenuModule,
    MatTableModule,
    MatCheckboxModule,
    MatSidenavModule,
    // Note to future self:  This is named NgxFileDropModule in later versions
    FileDropModule,
    DescriptionEditorModule
  ],
  declarations: [
    DataSourceToolbarComponent,
    DataSourceUploadComponent,
    DataSourceConfigComponent,
    FileExtensionIconComponent,
    CsvViewerComponent,
    TypedInstancePanelComponent,
    // Note: Leaving this in the app module, as it requires modularizing a bunch of stuff
    // DataExplorerComponent
  ],
  exports: [
    DataSourceToolbarComponent,
    FileExtensionIconComponent,
    CsvViewerComponent,
    TypedInstancePanelComponent
  ]
})
export class DataExplorerModule {
}

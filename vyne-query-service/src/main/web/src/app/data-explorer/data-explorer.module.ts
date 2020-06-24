import {NgModule} from '@angular/core';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {FileDropModule} from 'ngx-file-drop';
import {DataSourceToolbarComponent} from './data-source-toolbar.component';
import {DataSourceUploadComponent} from './data-source-upload.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {FormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatButtonModule} from '@angular/material/button';
import {DataSourceConfigComponent} from './data-source-config.component';
import {FileExtensionIconComponent} from './file-extension-icon.component';
import {MatMenuModule} from '@angular/material/menu';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatSelectModule} from '@angular/material/select';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {CsvViewerComponent} from './csv-viewer.component';
import {MatTableModule} from '@angular/material/table';
import {MatInputModule} from '@angular/material/input';
import {TypedInstancePanelComponent} from './typed-instance-panel.component';
import {MatSidenavModule} from '@angular/material/sidenav';
import {DescriptionEditorModule} from '../type-viewer/description-editor/description-editor.module';
import {AttributeTableModule} from '../type-viewer/attribute-table/attribute-table.module';
import {EnumTableModule} from '../type-viewer/enums-table/enum-table.module';
import {InlineQueryRunnerModule} from '../inline-query-runner/inline-query-runner.module';
import {ObjectViewModule} from '../object-view/object-view.module';
import {DataExplorerComponent} from './data-explorer.component';
import {MatTabsModule} from '@angular/material/tabs';
import {MatToolbarModule} from '@angular/material/toolbar';
import {SearchModule} from '../search/search.module';
import {CodeViewerModule} from '../code-viewer/code-viewer.module';
import {CovalentHighlightModule} from '@covalent/highlight';
import {TypedInstancePanelContainerComponent} from './typed-instance-panel-container.component';
import {InheritanceGraphModule} from '../inheritence-graph/inheritance-graph.module';
import { CaskPanelComponent } from './cask-panel.component';
import {MatExpansionModule} from '@angular/material/expansion';
import {MatProgressBarModule} from '@angular/material/progress-bar';


@NgModule({
  imports: [
    AttributeTableModule,
    EnumTableModule,
    InlineQueryRunnerModule,
    ObjectViewModule,
    CodeViewerModule,

    CommonModule,
    BrowserModule,
    FormsModule,
    BrowserAnimationsModule,
    TypeAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatToolbarModule,
    SearchModule,
    MatSelectModule,
    MatExpansionModule,
    MatTabsModule,
    MatMenuModule,
    MatTableModule,
    MatCheckboxModule,
    CovalentHighlightModule,
    MatSidenavModule,
    // Note to future self:  This is named NgxFileDropModule in later versions
    FileDropModule,
    DescriptionEditorModule,
    InheritanceGraphModule,
    MatProgressBarModule
  ],
  declarations: [
    DataSourceToolbarComponent,
    DataSourceUploadComponent,
    DataSourceConfigComponent,
    FileExtensionIconComponent,
    CsvViewerComponent,
    TypedInstancePanelContainerComponent,
    TypedInstancePanelComponent,
    DataExplorerComponent,
    CaskPanelComponent
  ],
  exports: [
    DataSourceToolbarComponent,
    FileExtensionIconComponent,
    CsvViewerComponent,
    TypedInstancePanelComponent,
    TypedInstancePanelContainerComponent,
    CaskPanelComponent
  ]
})
export class DataExplorerModule {
}

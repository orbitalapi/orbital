import { NgModule } from '@angular/core';
import { TypeAutocompleteModule } from '../type-autocomplete/type-autocomplete.module';
import { NgxFileDropModule } from 'ngx-file-drop';
import { DataSourceToolbarComponent } from './data-source-toolbar.component';
import { DataSourceUploadComponent } from './data-source-upload.component';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { DataSourceConfigComponent } from './data-source-config.component';
import { FileExtensionIconComponent } from './file-extension-icon.component';
import { MatLegacyMenuModule as MatMenuModule } from '@angular/material/legacy-menu';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacyCheckboxModule as MatCheckboxModule } from '@angular/material/legacy-checkbox';
import { CsvViewerComponent } from './csv-viewer.component';
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatSidenavModule } from '@angular/material/sidenav';
import { DescriptionEditorModule } from '../type-viewer/description-editor/description-editor.module';
import { AttributeTableModule } from '../type-viewer/attribute-table/attribute-table.module';
import { EnumTableModule } from '../type-viewer/enums-table/enum-table.module';
import { InlineQueryRunnerModule } from '../inline-query-runner/inline-query-runner.module';
import { ObjectViewModule } from '../object-view/object-view.module';
import { DataExplorerComponent } from './data-explorer.component';
import { MatLegacyTabsModule as MatTabsModule } from '@angular/material/legacy-tabs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { SearchModule } from '../search/search.module';
import { CodeViewerModule } from '../code-viewer/code-viewer.module';
import { CovalentHighlightModule } from '@covalent/highlight';
import { InheritanceGraphModule } from '../inheritence-graph/inheritance-graph.module';
import { CaskPanelComponent } from './cask-panel.component';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatLegacyProgressBarModule as MatProgressBarModule } from '@angular/material/legacy-progress-bar';
import { TypedInstancePanelModule } from '../typed-instance-panel/typed-instance-panel.module';
import { AgGridModule } from 'ag-grid-angular';
import { MatLegacyDialogModule as MatDialogModule } from '@angular/material/legacy-dialog';
import { AssignTypeToColumnDialogComponent } from './assign-types-dialog/assign-type-to-column-dialog.component';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { TypeNamePanelComponent } from './type-name-panel/type-name-panel.component';
import { SchemaGeneratorComponent } from './schema-generator-panel/schema-generator.component';
import { GridHeaderActionsComponent } from './custom-csv-table-header';
import { MatLegacyRadioModule as MatRadioModule } from '@angular/material/legacy-radio';
import { HeaderBarModule } from '../header-bar/header-bar.module';
import { TestPackModuleModule } from '../test-pack-module/test-pack-module.module';
import { MatIconModule } from '@angular/material/icon';
import { AngularSplitModule } from 'angular-split';
import { RouterModule } from '@angular/router';
import { AuthGuard } from 'src/app/services/auth.guard';
import { VynePrivileges } from 'src/app/services/user-info.service';
import { ResultsDownloadModule } from 'src/app/results-download/results-download.module';

@NgModule({
    imports: [
        AttributeTableModule,
        EnumTableModule,
        InlineQueryRunnerModule,
        ObjectViewModule,
        CodeViewerModule,
        CommonModule,
        FormsModule,
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
        DescriptionEditorModule,
        InheritanceGraphModule,
        MatProgressBarModule,
        TypedInstancePanelModule,
        AgGridModule,
        MatDialogModule,
        MatDatepickerModule,
        ReactiveFormsModule,
        MatRadioModule,
        HeaderBarModule,
        TestPackModuleModule,
        MatIconModule,
        NgxFileDropModule,
        AngularSplitModule,
        ResultsDownloadModule
    ],
    declarations: [
        DataSourceToolbarComponent,
        DataSourceUploadComponent,
        DataSourceConfigComponent,
        FileExtensionIconComponent,
        CsvViewerComponent,
        GridHeaderActionsComponent,
        DataExplorerComponent,
        CaskPanelComponent,
        AssignTypeToColumnDialogComponent,
        TypeNamePanelComponent,
        SchemaGeneratorComponent
    ],
    exports: [
        DataSourceToolbarComponent,
        FileExtensionIconComponent,
        CsvViewerComponent,
        CaskPanelComponent,
        DataSourceUploadComponent,
        DataSourceConfigComponent
    ]
})
export class DataExplorerModule {
}

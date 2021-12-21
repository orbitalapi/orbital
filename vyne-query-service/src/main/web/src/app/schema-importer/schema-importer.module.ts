import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SchemaImporterComponent} from './schema-importer.component';
import {
  TuiAccordionModule,
  TuiBadgeModule, TuiComboBoxModule, TuiDataListWrapperModule, TuiFilterByInputPipeModule,
  TuiInputModule, TuiSelectModule, TuiStringifyContentPipeModule, TuiTabsModule,
  TuiTagModule,
  TuiTextAreaModule,
  TuiTreeModule
} from '@taiga-ui/kit';
import {SchemaMemberListComponent} from './schema-member-list.component';
import {ModelMemberComponent} from './model-member.component';
import {ModelDisplayComponent} from './model-display.component';
import {FormsModule} from '@angular/forms';
import {
  TuiButtonModule,
  TuiDataListModule,
  TuiHintModule,
  TuiSvgModule,
  TuiTextfieldControllerModule
} from '@taiga-ui/core';
import {ModelMemberTreeNodeComponent} from './model-member-tree-node.component';
import {TypeSearchComponent} from './type-search/type-search.component';
import {TypeSearchResultComponent} from './type-search/type-search-result.component';
import {TypeSearchResultDocsComponent} from './type-search/type-search-result-docs.component';
import {TypeViewerModule} from '../type-viewer/type-viewer.module';
import {SchemaSourcePanelComponent} from './schema-source-panel/schema-source-panel.component';
import {NgSelectModule} from '@ng-select/ng-select';
import { SwaggerConfigComponent } from './schema-source-panel/config-panels/swagger-config.component';
import { JsonSchemaConfigComponent } from './schema-source-panel/config-panels/jsonschema-config.component';
import {DataExplorerModule} from '../data-explorer/data-explorer.module';
import {MatButtonModule} from '@angular/material/button';
import { DatabaseTableConfigComponent } from './schema-source-panel/config-panels/database-table-config.component';
import {DbConnectionEditorModule} from '../db-connection-editor/db-connection-editor.module';


@NgModule({
  exports: [SchemaImporterComponent, ModelDisplayComponent,
    TypeSearchComponent, TypeSearchResultComponent,
    SchemaSourcePanelComponent],
  declarations: [SchemaImporterComponent,
    SchemaMemberListComponent, ModelMemberComponent,
    ModelDisplayComponent, ModelMemberTreeNodeComponent,
    TypeSearchComponent, TypeSearchResultComponent, TypeSearchResultDocsComponent, SchemaSourcePanelComponent, SwaggerConfigComponent, JsonSchemaConfigComponent, DatabaseTableConfigComponent],
  imports: [
    CommonModule,
    DbConnectionEditorModule,
    TuiTreeModule,
    TuiAccordionModule,
    TuiBadgeModule,
    TuiTextAreaModule,
    TuiTextfieldControllerModule,
    FormsModule,
    TuiTagModule,
    TuiHintModule,
    TuiButtonModule,
    TuiInputModule,
    TypeViewerModule,
    TuiComboBoxModule,
    TuiDataListWrapperModule,
    TuiFilterByInputPipeModule,
    TuiStringifyContentPipeModule,
    NgSelectModule,
    TuiTabsModule,
    DataExplorerModule,
    MatButtonModule,
    TuiSelectModule,
    TuiSvgModule,
    TuiDataListModule
  ]
})
export class SchemaImporterModule {
}

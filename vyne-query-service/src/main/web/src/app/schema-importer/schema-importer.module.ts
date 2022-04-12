import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SchemaImporterComponent} from './schema-importer.component';
import {
  TuiAccordionModule,
  TuiBadgeModule,
  TuiCheckboxLabeledModule,
  TuiComboBoxModule,
  TuiDataListWrapperModule,
  TuiFilterByInputPipeModule,
  TuiInputModule,
  TuiSelectModule,
  TuiStringifyContentPipeModule,
  TuiTabsModule,
  TuiTagModule,
  TuiTextAreaModule,
  TuiTreeModule
} from '@taiga-ui/kit';
import {FormsModule} from '@angular/forms';
import {
  TuiButtonModule,
  TuiDataListModule, TuiGroupModule, TuiHintControllerModule,
  TuiHintModule,
  TuiLinkModule,
  TuiNotificationModule,
  TuiSvgModule,
  TuiTextfieldControllerModule
} from '@taiga-ui/core';
import {TypeViewerModule} from '../type-viewer/type-viewer.module';
import {SchemaSourcePanelComponent} from './schema-source-panel/schema-source-panel.component';
import {NgSelectModule} from '@ng-select/ng-select';
import {SwaggerConfigComponent} from './schema-source-panel/config-panels/swagger-config.component';
import {JsonSchemaConfigComponent} from './schema-source-panel/config-panels/jsonschema-config.component';
import {DataExplorerModule} from '../data-explorer/data-explorer.module';
import {MatButtonModule} from '@angular/material/button';
import {DatabaseTableConfigComponent} from './schema-source-panel/config-panels/database-table-config.component';
import {DbConnectionEditorModule} from '../db-connection-editor/db-connection-editor.module';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {SchemaExplorerTableComponent} from './schema-explorer-table/schema-explorer-table.component';
import {SchemaEntryTableComponent} from './schema-explorer-table/schema-entry-table.component';
import {OperationViewModule} from '../operation-view/operation-view.module';
import {KafkaTopicConfigComponent} from './schema-source-panel/config-panels/kafka-topic-config.component';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {ProtobufConfigComponent} from './schema-source-panel/config-panels/protobuf-config.component';
import {ConnectionFiltersModule} from "../utils/connections.pipe";


@NgModule({
  exports: [SchemaImporterComponent,
    SchemaExplorerTableComponent,
    SchemaSourcePanelComponent, KafkaTopicConfigComponent],
  declarations: [SchemaImporterComponent,
    SchemaEntryTableComponent, SchemaSourcePanelComponent,
    SwaggerConfigComponent, JsonSchemaConfigComponent,
    DatabaseTableConfigComponent, SchemaExplorerTableComponent, KafkaTopicConfigComponent,
    ProtobufConfigComponent
  ],
  imports: [
    ConnectionFiltersModule,
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
    TuiDataListModule,
    HeaderBarModule,
    TuiNotificationModule,
    TuiLinkModule,
    TuiCheckboxLabeledModule,
    OperationViewModule,
    TuiGroupModule,
    TuiHintControllerModule,
    TypeAutocompleteModule,
  ]
})
export class SchemaImporterModule {
}

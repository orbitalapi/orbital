import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SchemaImporterComponent} from './schema-importer.component';
import {
  TuiAccordionModule,
  TuiAvatarModule,
  TuiBadgeModule,
  TuiCheckboxLabeledModule,
  TuiCheckboxModule,
  TuiComboBoxModule,
  TuiDataListWrapperModule,
  TuiFilterByInputPipeModule,
  TuiInputModule,
  TuiRadioBlockModule,
  TuiSelectModule,
  TuiStringifyContentPipeModule,
  TuiTabsModule,
  TuiTagModule,
  TuiTextAreaModule,
  TuiTreeModule
} from '@taiga-ui/kit';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {
  TuiButtonModule,
  TuiDataListModule,
  TuiGroupModule,
  TuiHintModule,
  TuiLinkModule,
  TuiNotificationModule,
  TuiSvgModule,
  TuiTextfieldControllerModule
} from '@taiga-ui/core';
import {TypeViewerModule} from '../type-viewer/type-viewer.module';
import {SchemaSourcePanelComponent} from './schema-source-panel/schema-source-panel.component';
import {SwaggerConfigComponent} from './schema-source-panel/config-panels/swagger-config.component';
import {JsonSchemaConfigComponent} from './schema-source-panel/config-panels/jsonschema-config.component';
import {DataExplorerModule} from '../data-explorer/data-explorer.module';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';
import {DatabaseTableConfigComponent} from './schema-source-panel/config-panels/database-table-config.component';
import {DbConnectionEditorModule} from '../db-connection-editor/db-connection-editor.module';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {OperationViewModule} from '../operation-view/operation-view.module';
import {KafkaTopicConfigComponent} from './schema-source-panel/config-panels/kafka-topic-config.component';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {ProtobufConfigComponent} from './schema-source-panel/config-panels/protobuf-config.component';
import {ConnectionFiltersModule} from '../utils/connections.pipe';
import {AngularSplitModule} from 'angular-split';
import {RouterModule} from '@angular/router';
import {SchemaExplorerTableModule} from 'src/app/schema-explorer-table/schema-explorer-table.module';
import {AddSchemaTypeSelectorComponent} from './add-schema-type-selector/add-schema-type-selector.component';
import {OrbitalSchemaImporterContainerComponent} from './orbital-schema-importer-container.component';
import {PushSchemaConfigPanelComponent} from 'src/app/schema-importer/push-panel/push-schema-config-panel.component';
import {SchemaImporterContainerComponent} from './schema-importer-container.component';
import {CdPipelineInstructionsComponent} from './push-panel/cd-pipeline-instructions.component';
import {ApplicationPushInstructionsComponent} from './push-panel/application-push-instructions.component';
import {HeaderComponentLayoutModule} from 'src/app/header-component-layout/header-component-layout.module';
import {SchemaSourceConfigModule} from 'src/app/schema-source-config/schema-source-config.module';
import {ExpandingPanelSetModule} from "../expanding-panelset/expanding-panel-set.module";
import {CodeEditorModule} from "../code-editor/code-editor.module";
import {ProjectSelectorModule} from "../project-selector/project-selector.module";


@NgModule({
  exports: [SchemaImporterComponent,
    SchemaSourcePanelComponent, KafkaTopicConfigComponent,],
  declarations: [SchemaImporterComponent,
    SchemaSourcePanelComponent,
    SwaggerConfigComponent, JsonSchemaConfigComponent,
    DatabaseTableConfigComponent, KafkaTopicConfigComponent,
    ProtobufConfigComponent,
    AddSchemaTypeSelectorComponent,
    OrbitalSchemaImporterContainerComponent,
    PushSchemaConfigPanelComponent,
    SchemaImporterContainerComponent,
    CdPipelineInstructionsComponent,
    ApplicationPushInstructionsComponent,
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
    TuiTabsModule,
    DataExplorerModule,
    SchemaExplorerTableModule,
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
    TypeAutocompleteModule,
    AngularSplitModule,
    SchemaSourceConfigModule,
    RouterModule.forChild([
      {
        path: '',
        component: OrbitalSchemaImporterContainerComponent,
      },
    ]),
    TuiRadioBlockModule,
    TuiAvatarModule,
    ReactiveFormsModule,
    TuiCheckboxModule,
    HeaderComponentLayoutModule,
    ExpandingPanelSetModule,
    CodeEditorModule,
    ProjectSelectorModule
  ]
})
export class SchemaImporterModule {
}

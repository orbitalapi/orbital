import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs/internal/Observable';
import { TuiButtonModule, TuiDataListModule, TuiDropdownModule, TuiHintModule } from '@taiga-ui/core';
import {
  TuiDataListWrapperModule,
  TuiFilterByInputPipeModule,
  TuiIslandModule,
  TuiSelectModule,
  TuiStringifyContentPipeModule
} from '@taiga-ui/kit';
import { ConnectorSummary, MappedTable } from '../../db-connection-editor/db-importer.service';
import { ConvertSchemaEvent } from '../data-source-import.models';
import { Schema } from '../../services/schema';
import { SourcePackageDescription } from '../../package-viewer/packages.service';
import { ProjectSelectorModule } from '../../project-selector/project-selector.module';
import { SwaggerConfigComponent } from './config-panels/swagger-config.component';
import { JsonSchemaConfigComponent } from './config-panels/jsonschema-config.component';
import { DatabaseTableConfigComponent } from './config-panels/database-table-config.component';
import { KafkaTopicConfigComponent } from './config-panels/kafka-topic-config.component';
import { ProtobufConfigComponent } from './config-panels/protobuf-config.component';
import { ConnectionFiltersModule } from '../../utils/connections.pipe';
import { TuiClickOutsideModule } from '@taiga-ui/cdk';

@Component({
  selector: 'app-data-source-panel',
  styleUrls: ['./data-source-panel.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ProjectSelectorModule,
    TuiSelectModule,
    FormsModule,
    TuiDataListWrapperModule,
    TuiFilterByInputPipeModule,
    TuiStringifyContentPipeModule,
    TuiIslandModule,
    TuiButtonModule,
    SwaggerConfigComponent,
    JsonSchemaConfigComponent,
    DatabaseTableConfigComponent,
    KafkaTopicConfigComponent,
    ProtobufConfigComponent,
    ConnectionFiltersModule,
    TuiHintModule,
    TuiDataListModule,
    TuiDropdownModule,
    TuiClickOutsideModule
  ],
  template: `
    <div class="selectors">
      <app-project-selector
        prompt="Select a project to add the data source to"
        [packages]="packages"
        [(ngModel)]="selectedPackage"
        (ngModelChange)="isDataSourceSelectorOpen = true"
      ></app-project-selector>
      <tui-select
        tuiTextfieldSize="l"
        [(tuiDropdownOpen)]="isDataSourceSelectorOpen"
        [stringify]="stringify"
        [(ngModel)]="schemaType"
        [disabled]="!selectedPackage"
      >
        {{!selectedPackage ? 'Select a project first' : 'Select a data source to add'}}
        <tui-data-list *tuiDataList class="data-source-list">
          <button
            *ngFor="let item of schemaTypes"
            tuiOption
            [value]="item"
            [disabled]="item.isDisabled"
            [tuiHint]="item.isDisabled ? 'Coming soon!' : null"
            (click)="item.externalLink ? openSite(item.externalLink) : null; isDataSourceSelectorOpen = null"
          >
            <img [src]="item.icon"/>
            {{item.label}}
            <img *ngIf="item.externalLink" src="/assets/img/tabler/external-link.svg" class="external-link"/>
          </button>
        </tui-data-list>
      </tui-select>
    </div>
    <ng-container *ngIf="useIslandContainer && schemaType?.id && !schemaType?.externalLink; else forms">
      <tui-island class="island">
        <ng-container *ngTemplateOutlet="forms"></ng-container>
      </tui-island>
    </ng-container>
    <ng-template #forms>
      <div *ngIf="!schemaType?.externalLink" [ngSwitch]="schemaType?.id" class="config-container">
        <app-swagger-config *ngSwitchCase="'swagger'"
                            [packageIdentifier]="selectedPackage?.identifier"
                            (loadSchema)="convertSchema.emit({convertSchemaEvent: $event, dataSourceType: 'Swagger'})"
                            [working]="working">
        </app-swagger-config>
        <app-jsonschema-config *ngSwitchCase="'jsonSchema'"
                               [packageIdentifier]="selectedPackage?.identifier"
                               [working]="working"
                               (loadSchema)="convertSchema.emit({convertSchemaEvent: $event, dataSourceType: 'JSON'})">
        </app-jsonschema-config>
        <app-database-table-config [connections]="dbConnections | databases"
                                   *ngSwitchCase="'databaseTable'"
                                   [tables$]="tables$"
                                   [packageIdentifier]="selectedPackage?.identifier"
                                   (connectionChanged)="dbConnectionChanged.emit($event)"
                                   (loadSchema)="convertSchema.emit({convertSchemaEvent: $event, dataSourceType: 'Database'})"
                                   [working]="working"
        ></app-database-table-config>
        <app-kafka-topic-config [connections]="dbConnections | messageBrokers"
                                [schema]="schema"
                                [working]="working"
                                [packageIdentifier]="selectedPackage?.identifier"
                                (loadSchema)="convertSchema.emit({convertSchemaEvent: $event, dataSourceType: 'Kafka'})"
                                *ngSwitchCase="'kafkaTopic'"></app-kafka-topic-config>
        <app-protobuf-config [working]="working"
                             [packageIdentifier]="selectedPackage?.identifier"
                             (loadSchema)="convertSchema.emit({convertSchemaEvent: $event, dataSourceType: 'Protobuf'})"
                             *ngSwitchCase="'protobuf'"
        ></app-protobuf-config>
      </div>
    </ng-template>
  `
})
export class DataSourcePanelComponent {

  schemaTypes: SchemaType[] = [
    // { 'label' : 'Taxi', id: 'taxi'},
    { label: 'Swagger / OpenAPI', id: 'swagger', icon: '/assets/img/data-source-icons/open-api-icon.svg' },
    { label: 'Database table', id: 'databaseTable', icon: '/assets/img/tabler/database.svg' },
    { label: 'Kafka topic', id: 'kafkaTopic', icon: '/assets/img/data-source-icons/kafka-icon.svg' },
    { label: 'Protobuf', id: 'protobuf', icon: '/assets/img/data-source-icons/protobuf-icon.svg' },
    { label: 'DynamoDb', id: 'dynamodb', icon: '/assets/img/data-source-icons/aws-icon.svg', externalLink: 'https://orbitalhq.com/docs/describing-data-sources/aws-services#dynamo-db' },
    { label: 'Lambda', id: 'lambda', icon: '/assets/img/data-source-icons/aws-icon.svg', externalLink: 'https://orbitalhq.com/docs/describing-data-sources/aws-services#lambda' },
    { label: 'S3', id: 's3', icon: '/assets/img/data-source-icons/aws-icon.svg', externalLink: 'https://orbitalhq.com/docs/describing-data-sources/aws-services#s3' },
    { label: 'SQS', id: 'sqs', icon: '/assets/img/data-source-icons/aws-icon.svg', externalLink: 'https://orbitalhq.com/docs/describing-data-sources/aws-services#sqs' },
    { label: 'JsonSchema', id: 'jsonSchema', icon: '/assets/img/data-source-icons/json-icon.svg', isDisabled: true },
    // { 'label' : 'XML Schema (xsd)', id: 'xsd'},
  ]

  readonly stringify = (item: SchemaType) => item.label;

  schemaType: SchemaType
  selectedPackage: SourcePackageDescription
  isDataSourceSelectorOpen: boolean;

  @Input()
  packages: SourcePackageDescription[];

  @Input()
  working: boolean = false;

  @Input()
  dbConnections: ConnectorSummary[]

  @Input()
  tables$: Observable<MappedTable[]>;

  @Output()
  dbConnectionChanged = new EventEmitter<ConnectorSummary>();

  @Output()
  convertSchema = new EventEmitter<{convertSchemaEvent: ConvertSchemaEvent, dataSourceType: DataSourceType}>();

  @Input()
  schema: Schema

  @Input()
  useIslandContainer: boolean;

  openSite(siteUrl) {
    window.open(siteUrl, '_blank');
  }
}


export interface SchemaType {
  label: string;
  id: string;
  icon?: string;
  externalLink?: string
  isDisabled?: boolean
}

export type DataSourceType = 'Swagger' | 'Database' | 'Kafka' | 'Protobuf' | 'JSON';

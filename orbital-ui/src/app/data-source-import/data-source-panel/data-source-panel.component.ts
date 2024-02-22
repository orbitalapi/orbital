import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs/internal/Observable';
import { TuiButtonModule, TuiHintModule } from '@taiga-ui/core';
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

@Component({
  selector: 'app-data-source-panel',
  styleUrls: ['./data-source-panel.component.scss'],
  standalone: true,
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
    TuiHintModule
  ],
  template: `
    <div>
      <app-project-selector
        prompt="Select a project to add the data source to"
        [packages]="packages"
        [(ngModel)]="selectedPackage"
      ></app-project-selector>
      <tui-select
        *ngIf="dataSourceDisplayType === 'list'"
        [stringify]="stringify"
        [(ngModel)]="schemaType"
        [disabled]="!selectedPackage"
      >
        Select a data source to add
        <tui-data-list-wrapper
          *tuiDataList
          [items]="schemaTypes | tuiFilterByInputWith : stringify"
          [itemContent]="stringify | tuiStringifyContent"
        ></tui-data-list-wrapper>
      </tui-select>
      <div
        *ngIf="dataSourceDisplayType === 'buttons'"
        class="data-source-options-container"
      >
        <button
          *ngFor="let schemaT of schemaTypes"
          [disabled]="!selectedPackage || schemaT.isDisabled"
          [tuiHint]="schemaT.isDisabled ? 'Coming soon!' : null"
          [appearance]="schemaType === schemaT ? 'whiteblock-active' : 'whiteblock'"
          (click)="schemaT.externalLink ? openSite(schemaT.externalLink) : schemaType = schemaT"
          [iconRight]="schemaT.externalLink ? '/assets/img/tabler/external-link.svg' : null"
          tuiButton
          size="l"
        >
          <img *ngIf="schemaT.icon" [src]="schemaT.icon">
          {{ schemaT.label }}
        </button>
      </div>
      <ng-container *ngIf="useIslandContainer && schemaType?.id; else forms">
        <tui-island class="island">
          <ng-container *ngTemplateOutlet="forms"></ng-container>
        </tui-island>
      </ng-container>
      <ng-template #forms>
        <div [ngSwitch]="schemaType?.id" class="config-container">
          <app-swagger-config *ngSwitchCase="'swagger'"
                              [packageIdentifier]="selectedPackage?.identifier"
                              (loadSchema)="convertSchema.emit($event)"
                              [working]="working">
          </app-swagger-config>
          <app-jsonschema-config *ngSwitchCase="'jsonSchema'"
                                 [packageIdentifier]="selectedPackage?.identifier"
                                 [working]="working"
                                 (loadSchema)="convertSchema.emit($event)">
          </app-jsonschema-config>
          <app-database-table-config [connections]="dbConnections | databases"
                                     *ngSwitchCase="'databaseTable'"
                                     [tables$]="tables$"
                                     [packageIdentifier]="selectedPackage?.identifier"
                                     (connectionChanged)="dbConnectionChanged.emit($event)"
                                     (loadSchema)="convertSchema.emit($event)"
                                     [working]="working"
          ></app-database-table-config>
          <app-kafka-topic-config [connections]="dbConnections | messageBrokers"
                                  [schema]="schema"
                                  [working]="working"
                                  [packageIdentifier]="selectedPackage?.identifier"
                                  (loadSchema)="convertSchema.emit($event)"
                                  *ngSwitchCase="'kafkaTopic'"></app-kafka-topic-config>
          <app-protobuf-config [working]="working"
                               [packageIdentifier]="selectedPackage?.identifier"
                               (loadSchema)="convertSchema.emit($event)"
                               *ngSwitchCase="'protobuf'"
          ></app-protobuf-config>
        </div>
      </ng-template>
    </div>
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

  schemaType: SchemaType

  readonly stringify = (item: SchemaType) => item.label;

  selectedPackage: SourcePackageDescription

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
  convertSchema = new EventEmitter<ConvertSchemaEvent>();

  @Input()
  schema: Schema

  @Input()
  dataSourceDisplayType: 'list' | 'buttons' = 'list';

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

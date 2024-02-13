import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {ConnectorSummary, MappedTable} from '../../db-connection-editor/db-importer.service';
import {ConvertSchemaEvent} from '../schema-importer.models';
import {Observable} from 'rxjs/internal/Observable';
import {Schema} from '../../services/schema';
import {SourcePackageDescription} from "../../package-viewer/packages.service";

@Component({
  selector: 'app-schema-source-panel',
  styleUrls: ['./schema-source-panel.component.scss'],
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
          [disabled]="!selectedPackage"
          [appearance]="schemaType === schemaT ? 'whiteblock-active' : 'whiteblock'"
          (click)="schemaType = schemaT"
          tuiButton size="m"
          iconRight="tuiIconPlus"
        >
          {{schemaT.label}}
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
export class SchemaSourcePanelComponent {

  schemaTypes: SchemaType[] = [
    // { 'label' : 'Taxi', id: 'taxi'},
    {'label': 'Swagger / OpenAPI', id: 'swagger'},
    {'label': 'JsonSchema', id: 'jsonSchema'},
    {'label': 'Database table', id: 'databaseTable'},
    {'label': 'Kafka topic', id: 'kafkaTopic'},
    {'label': 'Protobuf', id: 'protobuf'},

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
  schema : Schema

  @Input()
  dataSourceDisplayType: 'list' | 'buttons' = 'list';

  @Input()
  useIslandContainer: boolean;

}


export interface SchemaType {
  label: string;
  id: string;
}

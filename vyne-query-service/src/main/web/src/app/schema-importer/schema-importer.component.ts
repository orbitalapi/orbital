import {Component, Input, OnInit} from '@angular/core';
import {SchemaSubmissionResult, TypesService} from '../services/types.service';
import {Operation, Schema, SchemaMember, Service, ServiceMember, Type} from '../services/schema';
import {ConnectorSummary, DbConnectionService, MappedTable} from '../db-connection-editor/db-importer.service';
import {ConvertSchemaEvent} from './schema-importer.models';
import {SchemaImporterService} from './schema-importer.service';
import {Observable} from 'rxjs/internal/Observable';
import {shareReplay} from 'rxjs/operators';
import {testImportForUI} from './schema-importer.data';


@Component({
  selector: 'app-schema-importer',
  styleUrls: ['./schema-importer.component.scss'],
  template: `
    <app-header-bar title="Schema Importer">
    </app-header-bar>
    <div class="page-content">
      <div class="importer-step step" *ngIf="wizardStep === 'importSchema'">
        <h2>Import a new schema</h2>
        <div class="form-container">
          <app-schema-source-panel
            [dbConnections]="connections"
            (dbConnectionChanged)="onDbConnectionChanged($event)"
            [tables$]="mappedTables$"
            (convertSchema)="convertSchema($event)"
            [working]="working"
          ></app-schema-source-panel>
          <tui-notification status="error" *ngIf="errorMessage">{{errorMessage}}
          </tui-notification>
        </div>
      </div>
      <div class="configuration-step step" *ngIf="wizardStep === 'configureTypes'">
        <h2>Configure the schema</h2>
        <app-schema-explorer-table [schemaSubmissionResult]="schemaSubmissionResult"
                                   [schema]="schema"
                                   (save)="saveSchema($event)"
        ></app-schema-explorer-table>
      </div>
    </div>`
})
export class SchemaImporterComponent {
  wizardStep: 'importSchema' | 'configureTypes' = 'configureTypes';

  connections: ConnectorSummary[];
  mappedTables$: Observable<MappedTable[]>;
  working: boolean = false;

  errorMessage: string;
  schemaSubmissionResult: SchemaSubmissionResult = testImportForUI as any;
  schema: Schema;

  constructor(private dbService: DbConnectionService,
              private schemaService: SchemaImporterService,
              private typeService: TypesService) {
    dbService.getConnections()
      .subscribe(connections => this.connections = connections);
    typeService.getTypes().subscribe(schema => this.schema = schema);
  }

  onDbConnectionChanged(selectedConnection: ConnectorSummary) {
    this.mappedTables$ = this.dbService.getMappedTablesForConnection(selectedConnection.connectionName)
      .pipe(shareReplay(1));
  }


  convertSchema($event: ConvertSchemaEvent) {
    this.working = true;
    this.schemaService.convertSchema($event).subscribe((result: SchemaSubmissionResult) => {
      this.schemaSubmissionResult = result;
      this.wizardStep = 'configureTypes';
      console.log(JSON.stringify(result));
    }, error => {
      console.error(JSON.stringify(error));
      this.errorMessage = error.error.message
    }, () => this.working = false);
  }

  saveSchema(schema: SchemaSubmissionResult) {
    this.schemaService.submitEditedSchema(schema)
      .subscribe(result => console.log(JSON.stringify(result)));
  }
}

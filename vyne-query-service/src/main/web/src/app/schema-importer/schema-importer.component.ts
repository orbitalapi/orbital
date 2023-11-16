import {Component, Input} from '@angular/core';
import {SchemaSubmissionResult, TypesService} from '../services/types.service';
import {Message, PartialSchema, Schema} from '../services/schema';
import {
  ConnectionsListResponse,
  ConnectorSummary,
  DbConnectionService,
  MappedTable
} from '../db-connection-editor/db-importer.service';
import {ConvertSchemaEvent} from './schema-importer.models';
import {SchemaEdit, SchemaEditOperation, SchemaImporterService} from './schema-importer.service';
import {Observable} from 'rxjs/internal/Observable';
import {shareReplay} from 'rxjs/operators';
import {appInstanceType} from 'src/app/app-config/app-instance.vyne';
import {PackagesService, SourcePackageDescription} from "../package-viewer/packages.service";


@Component({
  selector: 'app-schema-importer',
  styleUrls: ['./schema-importer.component.scss'],
  template: `
    <div class="importer-step step" *ngIf="wizardStep === 'importSchema'">
      <h2 *ngIf="title">Add a new schema</h2>
      <div class="form-container">
        <app-schema-source-panel
          [packages]="packages$ | async"
          [dbConnections]="connections.connections"
          (dbConnectionChanged)="onDbConnectionChanged($event)"
          [tables$]="mappedTables$"
          (convertSchema)="convertSchema($event)"
          [schema]="schema"
          [working]="working"
        ></app-schema-source-panel>
        <tui-notification status="error" *ngIf="schemaConversionError">{{schemaConversionError}}
        </tui-notification>
      </div>
    </div>
    <div class="configuration-step step" *ngIf="wizardStep === 'configureTypes'">
      <h2>Configure the schema</h2>
      <app-schema-explorer-table [partialSchema]="schemaSubmissionResult"
                                 [schema]="schema"
                                 [working]="working"
                                 [saveResultMessage]="schemaSaveResultMessage"
                                 [editable]="true"
                                 (save)="submitEdits($event)"
      ></app-schema-explorer-table>
    </div>
  `,
  host: {'class': appInstanceType.appType},
})
export class SchemaImporterComponent {
  wizardStep: 'importSchema' | 'configureTypes' = 'importSchema';

  packages$: Observable<SourcePackageDescription[]>

  @Input()
  title = 'Add a new schema';

  connections: ConnectionsListResponse;
  mappedTables$: Observable<MappedTable[]>;
  working: boolean = false;

  schemaConversionError: string;
  schemaSubmissionResult: SchemaSubmissionResult; // = testImportForUI as any;
  schema: Schema;
  schemaSaveResultMessage: Message;

  constructor(private dbService: DbConnectionService,
              private schemaService: SchemaImporterService,
              private typeService: TypesService,
              private packagesService: PackagesService,
  ) {
    this.packages$ = packagesService.listPackages();
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
      this.working = false;
    }, error => {
      console.error(JSON.stringify(error));
      this.schemaConversionError = error.error?.message || 'An error occurred';
      this.working = false;
    });
  }

  submitEdits(edits: SchemaEditOperation[]) {
    const schemaEdit: SchemaEdit = {
      packageIdentifier: this.schemaSubmissionResult.sourcePackage.identifier,
      edits: edits,
      dryRun: false
    }

    this.working = true;
    this.schemaService.submitSchemaEditOperation(schemaEdit)
      .subscribe(() => {
          this.working = false;
          this.schemaSaveResultMessage = {
            message: 'The schema was updated successfully',
            level: 'SUCCESS',
          };
        },
        error => {
          console.error(JSON.stringify(error));
          this.schemaSaveResultMessage = {
            message: error.error?.message || 'An error occurred',
            level: 'FAILURE',
          };
          this.working = false;
        },
      );
  }

  //
  // saveSchema(schema: PartialSchema) {
  //   this.working = true;
  //   this.schemaService.submitEditedSchema(schema)
  //     .subscribe(() => {
  //         this.working = false;
  //         this.schemaSaveResultMessage = {
  //           message: 'The schema was updated successfully',
  //           level: 'SUCCESS',
  //         };
  //       },
  //       error => {
  //         console.error(JSON.stringify(error));
  //         this.schemaSaveResultMessage = {
  //           message: error.error?.message || 'An error occurred',
  //           level: 'FAILURE',
  //         };
  //         this.working = false;
  //       },
  //     );
  // }
}

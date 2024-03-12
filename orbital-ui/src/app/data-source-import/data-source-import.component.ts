import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BehaviorSubject } from 'rxjs';
import { Observable } from 'rxjs/internal/Observable';
import { shareReplay } from 'rxjs/operators';
import { TuiNotificationModule } from '@taiga-ui/core';
import { SchemaSubmissionResult, TypesService } from '../services/types.service';
import { Message, Schema } from '../services/schema';
import {
  ConnectionsListResponse,
  ConnectorSummary,
  DbConnectionService,
  MappedTable
} from '../db-connection-editor/db-importer.service';
import { ConvertSchemaEvent } from './data-source-import.models';
import {
  CreateOrReplaceSource,
  SchemaEdit,
  SchemaEditOperation,
  SchemaImporterService
} from '../project-import/schema-importer.service';
import { appInstanceType } from 'src/app/app-config/app-instance.vyne';
import { PackagesService, SourcePackageDescription } from '../package-viewer/packages.service';
import { DataSourcePanelComponent } from './data-source-panel/data-source-panel.component';
import { SchemaMemberTypeExplorerModule } from '../schema-member-type-explorer/schema-member-type-explorer.module';
import { CodeViewerFlexBoxMode } from '../code-viewer/code-viewer.component';

@Component({
  selector: 'app-data-source-import',
  styleUrls: ['./data-source-import.component.scss'],
  standalone: true,
  imports: [CommonModule, DataSourcePanelComponent, TuiNotificationModule, SchemaMemberTypeExplorerModule],
  template: `
    <div class="importer-step step" *ngIf="(wizardStep | async) === 'importSchema'">
      <h2 *ngIf="title">{{ title }}</h2>
      <div class="form-container">
        <app-data-source-panel
          *ngIf="(packages$ | async) && connections"
          [packages]="packages$ | async"
          [dbConnections]="connections?.connections"
          (dbConnectionChanged)="onDbConnectionChanged($event)"
          [tables$]="mappedTables$"
          (convertSchema)="convertSchema($event)"
          [schema]="schema"
          [working]="working"
          [useIslandContainer]="useIslandContainer"
        ></app-data-source-panel>
        <tui-notification (close)="schemaConversionError = ''" status="error" *ngIf="schemaConversionError"
                          class="notification-error">{{ schemaConversionError }}
        </tui-notification>
      </div>
    </div>
    <div class="configuration-step step" *ngIf="(wizardStep | async) === 'configureTypes'">
      <h2>Configure the Data source</h2>
      <app-schema-member-type-explorer [partialSchema]="schemaSubmissionResult"
                                 [schema]="schema"
                                 [working]="working"
                                 [saveResultMessage]="schemaSaveResultMessage"
                                 [editable]="true"
                                 [codeViewerFlexBoxMode]="codeViewerFlexBoxMode"
                                 [useIslandContainer]="useIslandContainer"
                                 (save)="submitEdits($event)"
      ></app-schema-member-type-explorer>
    </div>
    <tui-notification
      [status]="schemaSaveResultMessage.level.toLowerCase()"
      *ngIf="schemaSaveResultMessage"
      class="notification-error"
      (close)="schemaSaveResultMessage = null"
    >
      {{ schemaSaveResultMessage.message }}
    </tui-notification>
  `,
  host: {'class': appInstanceType.appType},
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DataSourceImportComponent {
  wizardStep: BehaviorSubject<'importSchema' | 'configureTypes'> = new BehaviorSubject('importSchema');

  packages$: Observable<SourcePackageDescription[]>

  @Input()
  title = 'Add a new Data source';
  @Input()
  useIslandContainer: boolean;
  @Input()
  codeViewerFlexBoxMode: CodeViewerFlexBoxMode = 'flex';
  @Output()
  dataSourceSelected: EventEmitter<void> = new EventEmitter()
  @Output()
  dataSourceAdded: EventEmitter<void> = new EventEmitter()

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
              private changeDetector: ChangeDetectorRef
  ) {
    this.packages$ = packagesService.listPackages();
    dbService.getConnections()
      .subscribe(connections => {
        this.connections = connections
        this.changeDetector.markForCheck();
      });
    typeService.getTypes().subscribe(schema => this.schema = schema);
  }

  onDbConnectionChanged(selectedConnection: ConnectorSummary) {
    this.mappedTables$ = this.dbService.getMappedTablesForConnection(selectedConnection.connectionName)
      .pipe(shareReplay(1));
  }


  convertSchema($event: ConvertSchemaEvent) {
    this.working = true;
    this.schemaService.convertSchema($event).subscribe({
      next: (result: SchemaSubmissionResult<CreateOrReplaceSource>) => {
        this.schemaSubmissionResult = result;
        this.wizardStep.next('configureTypes');
        console.log(JSON.stringify(result, null, 2));
        this.working = false;
        this.dataSourceSelected.emit();
        this.changeDetector.markForCheck();
      },
      error: (error) => {
        console.error(JSON.stringify(error, null, 2));
        this.schemaConversionError = error.error?.message || error.message || 'An error occurred';
        this.working = false;
        this.changeDetector.markForCheck();
      }
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
          this.wizardStep.next('importSchema');
          this.dataSourceAdded.emit();
          this.changeDetector.markForCheck();
        },
        error => {
          console.error(JSON.stringify(error));
          this.schemaSaveResultMessage = {
            message: error.error?.message || 'An error occurred',
            level: 'ERROR',
          };
          this.working = false;
          this.changeDetector.markForCheck();
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

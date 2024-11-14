import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter, Inject, Injector,
  Input,
  OnDestroy, OnInit,
  Output
} from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {BehaviorSubject} from 'rxjs';
import {Observable} from 'rxjs/internal/Observable';
import {shareReplay} from 'rxjs/operators';
import {TuiAlertService, TuiButton, TuiNotification} from '@taiga-ui/core';
import {SchemaSubmissionResult, TypesService} from '../services/types.service';
import {Message, Schema} from '../services/schema';
import {
  ConnectionsListResponse,
  ConnectorSummary,
  DbConnectionService,
  MappedTable
} from '../db-connection-editor/db-importer.service';
import {ConvertSchemaEvent} from './data-source-import.models';
import {
  CreateOrReplaceSource,
  SchemaEdit,
  SchemaEditOperation,
  SchemaImporterService
} from '../project-import/schema-importer.service';
import {appInstanceType} from 'src/app/app-config/app-instance.vyne';
import {PackagesService, SourcePackageDescription} from '../package-viewer/packages.service';
import {DataSourcePanelComponent, DataSourceType} from './data-source-panel/data-source-panel.component';
import {SchemaMemberTypeExplorerModule} from '../schema-member-type-explorer/schema-member-type-explorer.module';
import {CodeViewerFlexBoxMode} from '../code-viewer/code-viewer.component';
import {showAlertForMessage} from "../alert-with-dismiss/alert-with-dismiss.component";

@Component({
  selector: 'app-data-source-import',
  styleUrls: ['./data-source-import.component.scss'],
  standalone: true,
  imports: [CommonModule, DataSourcePanelComponent, TuiNotification, SchemaMemberTypeExplorerModule, NgIf, TuiButton],
  template: `
    <div class="importer-step step" *ngIf="(wizardStep | async) === 'importSchema'">
      <h3 *ngIf="title">{{ title }}</h3>
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
        <tui-notification appearance="error"
                          *ngIf="schemaConversionError"
                          class="notification-error"
        >
          {{ schemaConversionError }}
          <button tuiIconButton iconStart="@tui.x" type="button" (click)="schemaConversionError = ''">Close</button>
        </tui-notification>
      </div>
    </div>
    <div class="configuration-step step" *ngIf="(wizardStep | async) === 'configureTypes'">
      <h3>Link your data & services</h3>
      <div class="instructions">Here's the {{ dataSourceType }} data source we just imported. Take a moment to build
        links to other data sources, by updating your types to existing, shared types.
      </div>
      <app-schema-member-type-explorer [partialSchema]="schemaSubmissionResult"
                                       [schema]="schema"
                                       [working]="working"
                                       [saveResultMessage]="schemaSaveResultMessage"
                                       [editable]="true"
                                       [codeViewerFlexBoxMode]="codeViewerFlexBoxMode"
                                       (save)="submitEdits($event)"
                                       (cancelConfig)="onCancelConfig()"
      ></app-schema-member-type-explorer>
    </div>
    <tui-notification
      *ngIf="schemaSaveResultMessage?.severity === 'FAILURE'"
      appearance="error"
      class="notification-error"
    >
      {{ schemaSaveResultMessage?.message }}
      <button tuiIconButton iconStart="@tui.x" type="button" (click)="schemaSaveResultMessage = null">Close</button>
    </tui-notification>
  `,
  host: {'class': appInstanceType.appType},
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DataSourceImportComponent implements OnInit, OnDestroy {
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
  @Output() onConfigureStep = new EventEmitter<boolean>();

  connections: ConnectionsListResponse;
  mappedTables$: Observable<MappedTable[]>;
  working: boolean = false;

  schemaConversionError: string;
  schemaSubmissionResult: SchemaSubmissionResult; // = testImportForUI as any;
  schema: Schema;
  schemaSaveResultMessage: Message;
  dataSourceType: DataSourceType;

  constructor(private dbService: DbConnectionService,
              private schemaService: SchemaImporterService,
              private typeService: TypesService,
              private packagesService: PackagesService,
              private changeDetector: ChangeDetectorRef,
              private router: Router,
              private activatedRoute: ActivatedRoute,
              private alerts: TuiAlertService,
              @Inject(Injector) private readonly injector: Injector,
  ) {
    this.packages$ = packagesService.listPackages();
    dbService.getConnections()
      .subscribe(connections => {
        this.connections = connections
        this.changeDetector.markForCheck();
      });
    typeService.getTypes().subscribe(schema => this.schema = schema);
  }

  ngOnInit() {
    // ensures we don't start on the /data-source/configure route
    this.resetToBaseRoute();
  }

  ngOnDestroy(): void {
    this.onConfigureStep.emit(false);
  }

  onDbConnectionChanged(selectedConnection: ConnectorSummary) {
    this.mappedTables$ = this.dbService.getMappedTablesForConnection(selectedConnection.connectionName)
      .pipe(shareReplay(1));
  }


  convertSchema(event: { convertSchemaEvent: ConvertSchemaEvent, dataSourceType: DataSourceType }) {
    this.working = true;
    this.schemaService.convertSchema(event.convertSchemaEvent).subscribe({
      next: (result: SchemaSubmissionResult<CreateOrReplaceSource>) => {
        this.schemaSubmissionResult = result;
        this.dataSourceType = event.dataSourceType
        this.wizardStep.next('configureTypes');
        this.onConfigureStep.emit(true);
        this.router.navigate(['configure'], {relativeTo: this.activatedRoute, replaceUrl: true});
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
            severity: 'SUCCESS',
          };
          this.onConfigureStep.emit(false);
          this.wizardStep.next('importSchema');
          this.dataSourceAdded.emit();
          this.resetToBaseRoute();
          this.changeDetector.markForCheck();
        },
        error => {
          console.error(JSON.stringify(error));
          this.schemaSaveResultMessage = {
            message: error.error?.message || 'An error occurred',
            severity: 'ERROR',
          };
          this.working = false;
          this.changeDetector.markForCheck();
        },
      );
  }

  onCancelConfig() {
    this.onConfigureStep.emit(false);
    this.wizardStep.next('importSchema');
    this.resetToBaseRoute();
  }

  private resetToBaseRoute() {
    this.router.navigate(
      ['../data-source'],
      {
        replaceUrl: true,
        relativeTo: this.activatedRoute,
      }
    );
  }
}

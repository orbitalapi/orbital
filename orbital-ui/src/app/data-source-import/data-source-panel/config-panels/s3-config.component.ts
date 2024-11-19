import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {RouterLink} from '@angular/router';
import {TuiLet} from '@taiga-ui/cdk';
import {TuiComboBoxModule, TuiInputModule, TuiSelectModule} from '@taiga-ui/legacy';
import {ChangeDetectorRef, Component, EventEmitter, Inject, Injector, Input, Output} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  TuiDialogService,
  TuiDataList,
  TuiIcon,
  TuiButton,
  TuiGroup,
  TuiLoader,
  TuiNotification,
  TuiTextfield,
} from '@taiga-ui/core';
import {TuiDataListWrapper, TuiButtonLoading, TuiSegmented, TuiFilterByInputPipe} from '@taiga-ui/kit';
import { PolymorpheusComponent } from '@taiga-ui/polymorpheus';
import {combineLatestWith, finalize, Observable, of, Subject, switchMap} from 'rxjs';
import {catchError, debounceTime, tap} from 'rxjs/operators';
import { ConnectorSummary } from '../../../db-connection-editor/db-importer.service';
import { TypeAutocompleteTuiModule } from '../../../type-autocomplete-tui/type-autocomplete-tui.module';
import {AwsConnectionsPipe} from '../../../utils/connections.pipe';
import {isNullOrUndefined, sanitiseNamespace} from '../../../utils/utils';
import {ConvertSchemaEvent, S3SchemaConverterOptions,} from '../../data-source-import.models';
import {Schema, Type} from '../../../services/schema';
import {
  ConnectionEditorContext,
  DbConnectionEditorDialogComponent
} from '../../../db-connection-editor/db-connection-editor-dialog.component';
import { PackageIdentifier } from '../../../package-viewer/packages.service';
import { UiCustomisations } from '../../../../environments/ui-customisations';
import {ListBucketsResponse, ListObjectsResponse, S3ConnectionService} from './s3-importer.service';

@Component({
  selector: 'app-s3-config',
  standalone: true,
  imports: [
    CommonModule,
    TuiSelectModule,
    FormsModule,
    TuiDataListWrapper,
    TuiButton,
    TuiInputModule,
    TuiDataList,
    TuiIcon,
    TypeAutocompleteTuiModule,
    TuiButtonLoading,
    AwsConnectionsPipe,
    TuiGroup,
    TuiSegmented,
    TuiLoader,
    TuiNotification,
    TuiComboBoxModule,
    TuiTextfield,
    TuiFilterByInputPipe,
    TuiLet,
    RouterLink,
  ],
  template: `
    <ng-container *tuiLet="fileList$ | async as fileList">
      <div class="form-container">
        <form class="form-body" #s3Form="ngForm">
          <div class="form-row">
            <div class="form-item-description-container">
              <h3>Connection</h3>
              <div class="help-text">
                Select the AWS connection for the S3 bucket you wish to connect to
              </div>
            </div>
            <div class="form-element">
              <tui-select
                [stringify]="stringifyConnection"
                [(ngModel)]="selectedConnection"
                (ngModelChange)="onAwsConnectionSelected($event)"
                name="connection"
                required
              >
                Connection name
                <span class="tui-required"></span>
                <tui-data-list *tuiDataList>
                  <button
                    tuiOption
                    class="link"
                    (click)="createNewConnection()"
                  >
                    <tui-icon icon="@tui.circle-plus" class="icon"></tui-icon>
                    Add new connection...
                  </button>
                  <button *ngFor="let connection of connections | awsConnections" tuiOption
                          [value]="connection">{{ connection.connectionName }}
                  </button>
                </tui-data-list>
              </tui-select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-item-description-container">
              <h3>Namespace</h3>
              <div class="help-text">
                Defines a namespace which the newly exposed Service and Operation will be defined in.
              </div>
            </div>
            <div class="form-element">
              <tui-input [(ngModel)]="s3Options.targetNamespace" name="namespace" required>
                Default namespace
                <span class="tui-required"></span>
              </tui-input>
            </div>
          </div>
          <div class="form-row">
            <div class="form-item-description-container">
              <h3>Bucket name</h3>
              <div class="help-text">
                Select a bucket name from the AWS region specified in the connection
              </div>
            </div>
            <div class="form-element">
              <tui-combo-box
                tuiTextfieldSize="m"
                [(ngModel)]="s3Options.bucketName"
                (ngModelChange)="bucketNameSubject$.next($event)"
                [disabled]="!s3Options.connectionName"
                name="bucket"
                *tuiLet="bucketList$ | async as bucketList"
                [required]="true"
              >
                Bucket name
                <span class="tui-required"></span>
                <input
                  placeholder="Search bucket names"
                  tuiTextfieldLegacy
                  (input)="bucketNameSubject$.next(extractValueFromEvent($event))"
                />
                <tui-data-list-wrapper
                  *tuiDataList
                  [items]="bucketList?.bucketNames | tuiFilterByInput"
                />
              </tui-combo-box>
              <tui-notification *ngIf="bucketListErrorMessage" appearance="negative">
                {{ bucketListErrorMessage }}
              </tui-notification>
            </div>
          </div>
          <div class="form-row">
            <div class="form-item-description-container">
              <h3>Filename pattern</h3>
              <div class="help-text">
                Specify a pattern to select which files from the bucket are read. Eg: * (everything), *.csv (all csv's), or films.csv (a single file)
              </div>
            </div>
            <div class="form-element filename-pattern-input">
              <tui-input
                [(ngModel)]="s3Options.filenamePattern"
                (ngModelChange)="filenamePatternSubject$.next($event);"
                [disabled]="!s3Options.bucketName"
                name="filenamePattern"
                required
              >
                Filename pattern
                <span class="tui-required"></span>
                <input tuiTextfieldLegacy placeholder="(eg. *.csv)" />
              </tui-input>
              <tui-loader [showLoader]="isFileListLoading" class="filename-pattern-loader"></tui-loader>
              <tui-notification *ngIf="fileListErrorMessage" appearance="negative">
                {{ fileListErrorMessage }}
              </tui-notification>
              <tui-notification *ngIf="!isFileListLoading && fileList"
                                [appearance]="fileList?.objectNames.length ? 'info' : 'warning'"
                                size="m"
              >
                @if (fileList?.objectNames.length) {
                  <div>The pattern matches {{ fileList?.objectNames.length | i18nPlural: fileListPluralMap }}:</div>
                  <div class="file-list-container">
                    <div *ngFor="let file of fileList?.objectNames">{{ file }}</div>
                  </div>
                } @else {
                  No files matched that pattern.
                }
              </tui-notification>
            </div>
          </div>
          <div class="form-row">
            <div class="form-item-description-container">
              <h3>Data model</h3>
              <div class="help-text">
                Select a data model that matches the contents of your files.
              </div>
            </div>
            <div class="form-element">
              <tui-segmented size="m" [(activeItemIndex)]="messageTypeModeIndex">
                <button tuiButton appearance="flat-grayscale" size="m" [disabled]="!s3Options.bucketName">
                  Existing model
                </button>
                <button tuiButton appearance="flat-grayscale" size="m" [disabled]="!s3Options.bucketName">
                  New model
                </button>
              </tui-segmented>
              <app-type-autocomplete-tui
                *ngIf="messageTypeModeIndex === 0"
                class="type-input"
                label="Data model"
                size="l"
                [schema]="schema"
                (selectedTypeChanged)="onFileSchemaTypeSelected($event?.member)"
                [isRequired]="true"
                [isDisabled]="!s3Options.bucketName"
                ngModelGroup="messageType"
              >
              </app-type-autocomplete-tui>
              <ng-container *ngIf="messageTypeModeIndex === 1">
                <tui-notification appearance="info" size="m">
                  A new data model will be created. <br/> Specify a name for your new model, and select a file to parse to generate the model from.
                </tui-notification>
                <div tuiGroup class="horizontal-group">
                  <div>
                    <tui-input
                      [(ngModel)]="s3Options.newModelName"
                      name="newTypeName"
                      required
                      [style.border-radius]="'inherit'"
                      [disabled]="!s3Options.filenamePattern"
                    >
                      Model name
                      <span class="tui-required"></span>
                    </tui-input>
                  </div>
                  <div>
                    <tui-combo-box
                      tuiTextfieldSize="m"
                      [(ngModel)]="s3Options.fileToGenerateSchemaFrom"
                      name="fileToGenerateSchemaFrom"
                      [tuiTextfieldCleaner]="true"
                      [required]="true"
                      [style.border-radius]="'inherit'"
                      [disabled]="!s3Options.filenamePattern"
                    >
                      File
                      <span class="tui-required"></span>
                      <input
                        placeholder="Search matched files"
                        tuiTextfieldLegacy
                      />
                      <tui-data-list-wrapper
                        *tuiDataList
                        [items]="fileList?.objectNames | tuiFilterByInput"
                      />
                    </tui-combo-box>
                  </div>
                </div>
                <tui-notification *ngIf="!isFileListLoading && !fileList?.objectNames.length"
                                  appearance="warning"
                                  size="m"
                >
                  A source file is required to generate a new type. Upload a file to the bucket, adjust your filter, or
                  design a new type in the <a routerLink="/designer">designer</a>.
                </tui-notification>
              </ng-container>
            </div>
          </div>
          <div class="form-row">
            <div class="form-item-description-container">
              <h3>Service and Operation name</h3>
              <div class="help-text">
                Specify a name for the service and operation that will be created for this topic.
                It's ok to leave these blank - a name will be generated for you.
              </div>
            </div>
            <div class="form-element">
              <div tuiGroup class="horizontal-group">
                <div>
                  <tui-input
                    [(ngModel)]="s3Options.serviceName"
                    name="serviceName"
                    [disabled]="!s3Options.bucketName"
                  >
                    Service name
                  </tui-input>
                </div>
                <div>
                  <tui-input
                    [(ngModel)]="s3Options.operationName"
                    name="operationName"
                    [disabled]="!s3Options.bucketName"
                  >
                    Operation name
                  </tui-input>
                </div>
              </div>
            </div>
          </div>
        </form>
      </div>

      <div class="form-button-bar">
        <button tuiButton [loading]="working" (click)="doCreate()" [size]="'m'" [disabled]="s3Form.invalid">Configure
        </button>
      </div>
    </ng-container>
  `,
  styleUrls: ['./s3-config.component.scss']
})
export class S3ConfigComponent {
  @Input()
  connections: ConnectorSummary[] = [];
  selectedConnection: ConnectorSummary = null;

  @Output()
  loadSchema = new EventEmitter<ConvertSchemaEvent>()

  @Input()
  working: boolean = false;

  @Input()
  schema: Schema;

  @Input()
  packageIdentifier: PackageIdentifier;

  messageTypeModeIndex = 0
  connectionNameSubject$ = new Subject<string>()
  bucketNameSubject$ = new Subject<string>()
  filenamePatternSubject$ = new Subject<string>();
  bucketList$: Observable<ListBucketsResponse>;
  fileList$: Observable<ListObjectsResponse>;
  fileListErrorMessage: string
  isFileListLoading: boolean
  bucketListErrorMessage: string

  readonly stringifyConnection = (item: ConnectorSummary) => item.connectionName;
  readonly uiConfig = UiCustomisations;
  s3Options: S3SchemaConverterOptions = new S3SchemaConverterOptions();

  fileListPluralMap: {[k: string]: string} = {
    '=1' : '1 file',
    'other' : '# files'
  }

  constructor(
    private s3ImporterService: S3ConnectionService,
    @Inject(Injector) private readonly injector: Injector,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
    private changeDetector: ChangeDetectorRef
  ) {
    this.bucketList$ = this.connectionNameSubject$.pipe(
      switchMap(connectionName => this.s3ImporterService.listBuckets(this.packageIdentifier.uriSafeId, connectionName).pipe(
        catchError(e => {
          this.bucketListErrorMessage = e.error?.message || 'an error occurred'
          return of(null);
        })
      ))
    )
    this.fileList$ = this.filenamePatternSubject$
      .pipe(
        debounceTime(300),
        combineLatestWith(this.connectionNameSubject$, this.bucketNameSubject$),
        tap(([pattern]) => {
          this.fileListErrorMessage = null
          this.isFileListLoading = !!pattern;
          this.changeDetector.markForCheck()
        }),
        takeUntilDestroyed(),
        switchMap(([pattern, connectionName, bucketName]) => this.s3ImporterService.listFilenames(this.packageIdentifier.uriSafeId, connectionName, bucketName, pattern)
          .pipe(
            tap(value => {
              if (value.objectNames.length === 0) {
                this.s3Options.fileToGenerateSchemaFrom = null
              }
            }),
            catchError(e => {
              this.fileListErrorMessage = e.error?.message || 'an error occurred'
              return of(null);
            }),
            finalize(() => {
              this.isFileListLoading = false
              this.changeDetector.markForCheck()
            })
          )
        ),
      )
  }

  doCreate() {
    console.log(this.s3Options);
    // clear out options that weren't selected
    const clonedS3Options = JSON.parse(JSON.stringify(this.s3Options)) as S3SchemaConverterOptions
    if (this.messageTypeModeIndex === 0) {
      clonedS3Options.fileToGenerateSchemaFrom = clonedS3Options.newModelName = null
    } else if (this.messageTypeModeIndex === 1) {
      // concatenate this here for the server
      clonedS3Options.fileSchemaType = this.s3Options.targetNamespace + '.' + this.s3Options.newModelName
      clonedS3Options.filenamePattern = null
    }
    //remove any null entries
    Object.keys(clonedS3Options).forEach((k) => clonedS3Options[k] == null && delete clonedS3Options[k]);
    this.loadSchema.next(new ConvertSchemaEvent('s3', clonedS3Options, this.packageIdentifier));
  }

  createNewConnection() {
    this.dialogService.open<ConnectorSummary>(new PolymorpheusComponent(DbConnectionEditorDialogComponent, this.injector),
      {
        data: new ConnectionEditorContext('AWS', null, 'edit', this.packageIdentifier),
        size: 'l'
      })
      .subscribe((result: ConnectorSummary) => {
        this.connections.push(result);
        this.selectedConnection = result;
        this.s3Options.connectionName = result.connectionName
      })
  }

  onAwsConnectionSelected($event: any) {
    const { organisation, name } = this.packageIdentifier;
    this.s3Options.targetNamespace = sanitiseNamespace(`${organisation}.${name}.${$event.connectionName}`);
    this.s3Options.connectionName = $event.connectionName
    this.connectionNameSubject$.next($event.connectionName)
  }

  onFileSchemaTypeSelected(member: Type) {
    if (isNullOrUndefined(member)) {
      this.s3Options.fileSchemaType = null
    } else {
      this.s3Options.fileSchemaType = member.name.parameterizedName
    }
  }

  protected extractValueFromEvent(event: Event): string | null {
    return (event.target as HTMLInputElement)?.value || null;
  }
}

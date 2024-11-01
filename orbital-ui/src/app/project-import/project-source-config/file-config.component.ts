import { TuiButtonLoading } from "@taiga-ui/kit";
import { TuiSelectModule } from "@taiga-ui/legacy";
import {CommonModule} from '@angular/common';
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {projectTypeToString} from 'src/app/project-import/project-source-config/git-config.component';
import {
  AvroPackageLoaderSpec,
  FileSystemPackageSpec,
  LoadablePackageType,
  OpenApiPackageLoaderSpec,
  TaxiPackageLoaderSpec
} from '../project-import.models';
import {Message} from 'src/app/services/schema';
import {FileRepositoryTestResponse, SchemaImporterService} from 'src/app/project-import/schema-importer.service';
import { TuiAlertService, TuiNotification, TuiDataList, TuiButton } from '@taiga-ui/core';
import {Router} from "@angular/router";
import {AvroPackageConfigComponent} from './avro-package-config.component';
import {OpenApiPackageConfigComponent} from './open-api-package-config.component';
import {TaxiPackageConfigComponent} from './taxi-package-config.component';

@Component({
  selector: 'app-file-config',
  standalone: true,
  template: `
    <div class='form-header-text' *ngIf="editable">
      <p>Read projects directly from the local machine.</p>
      <tui-notification status='warning'>Disk based projects are great for getting started and dev-local
        experiments,
        however you should consider storing your project in a git repository in production
      </tui-notification>
    </div>
    <form #fileForm='ngForm'>
      <div class='form-container'>
        <div class='form-body'>
          <div class='form-row'>
            <div class='form-item-description-container'>
              <h3>Project type</h3>
              <div class='help-text'>
                Add either a full Taxi project, or individual API specs
              </div>
            </div>
            <div class='form-element'>
              <tui-select
                [readOnly]='!editable'
                [stringify]='stringifyProjectType'
                [ngModel]='fileSystemPackageConfig.loader.packageType'
                (ngModelChange)='selectedProjectTypeChanged($event)'
                name='project-type' required
              >
                Project type
                <tui-data-list *tuiDataList>
                  <button tuiOption value='Taxi'>{{ stringifyProjectType('Taxi') }}</button>
                  <button tuiOption value='OpenApi'>{{ stringifyProjectType('OpenApi') }}</button>
                  <button tuiOption value='Avro'>{{ stringifyProjectType('Avro') }}</button>
                </tui-data-list>
              </tui-select>
            </div>
          </div>
          <app-taxi-package-config
            *ngIf="fileSystemPackageConfig.loader.packageType === 'Taxi'"
            [fileSystemPackageConfig]="fileSystemPackageConfig"
            [editable]="editable"
            ngModelGroup="taxiForm"
          ></app-taxi-package-config>
          <app-open-api-package-config
            *ngIf="fileSystemPackageConfig.loader.packageType ==='OpenApi'"
            [openApiPackageSpec]='openApiPackageSpec'
            [projectType]="'file'"
            [editable]="editable"
            [(path)]='fileSystemPackageConfig.path'
            (fileChange)="filePayload = $event"
            ngModelGroup="openApiForm"
          ></app-open-api-package-config>
          <app-avro-package-config
            *ngIf="fileSystemPackageConfig.loader.packageType === 'Avro'"
            [packageSpec]="avroPackageSpec"
            [projectType]="'file'"
            [editable]="editable"
            [(path)]='fileSystemPackageConfig.path'
            (fileChange)="filePayload = $event"
            ngModelGroup="avroForm"
          ></app-avro-package-config>
        </div>
      </div>
    </form>
    <div *ngIf='editable' class='form-button-bar'>
      <button
        tuiButton
        appearance="secondary"
        [size]="'m'"
        (click)="goBackOnboarding.emit()"
      >Cancel
      </button>
      <button tuiButton [loading]='working' [size]="'m'" (click)='doCreate()' [disabled]='fileForm.invalid'>
        Create
      </button>
    </div>
    <tui-notification [appearance]='saveResultMessage.severity.toLowerCase()' *ngIf='saveResultMessage'>
      {{ saveResultMessage.message }}
    </tui-notification>
  `,
  styleUrls: ['./file-config.component.scss'],
  imports: [
    TuiNotification,
    FormsModule,
    TuiSelectModule,
    TuiDataList,
    TaxiPackageConfigComponent,
    OpenApiPackageConfigComponent,
    AvroPackageConfigComponent,
    TuiButton,
    CommonModule,
    TuiButtonLoading
],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FileConfigComponent {
  @Input()
  fileSystemPackageConfig: FileSystemPackageSpec = new FileSystemPackageSpec();
  @Input()
  editable: boolean = true;
  @Input()
  isOnboardingMode: boolean;
  @Output()
  goBackOnboarding: EventEmitter<void> = new EventEmitter();
  @Output()
  localFileAdded: EventEmitter<void> = new EventEmitter()

  readonly stringifyProjectType = projectTypeToString;
  filePathTestResult: FileRepositoryTestResponse;
  working = false;
  saveResultMessage: Message;
  filePayload: string;

  constructor(
    private changeDetector: ChangeDetectorRef,
    private schemaService: SchemaImporterService,
    private alertsService: TuiAlertService,
    private router: Router
  ) {}

  get avroPackageSpec():AvroPackageLoaderSpec | null {
    const packageType = this.fileSystemPackageConfig.loader?.packageType;
    if (packageType === 'Avro') {
      return this.fileSystemPackageConfig.loader as AvroPackageLoaderSpec;
    } else {
      return null;
    }
  }

  get openApiPackageSpec(): OpenApiPackageLoaderSpec | null {
    const packageType = this.fileSystemPackageConfig.loader?.packageType;
    if (packageType === 'OpenApi') {
      return this.fileSystemPackageConfig.loader as OpenApiPackageLoaderSpec;
    } else {
      return null;
    }
  }

  selectedProjectTypeChanged(projectType: LoadablePackageType) {
    this.saveResultMessage = null;
    switch (projectType) {
      case 'Taxi':
        this.fileSystemPackageConfig.loader = new TaxiPackageLoaderSpec();
        break;
      case 'OpenApi':
        this.fileSystemPackageConfig.loader = new OpenApiPackageLoaderSpec();
        break;
      case "Avro":
        this.fileSystemPackageConfig.loader = new AvroPackageLoaderSpec();
    }
    delete this.fileSystemPackageConfig.newProjectIdentifier
    this.changeDetector.markForCheck();
  }

  doCreate() {
    this.working = true;
    this.saveResultMessage = null;
    if (this.fileSystemPackageConfig.loader.packageType === 'Taxi') {
      this.schemaService.addNewFileRepository(this.fileSystemPackageConfig)
        .subscribe({
          next: (result) => this.creationSuccess(result),
          error: (error) => this.creationError(error)
        });
    } else if (this.fileSystemPackageConfig.loader.packageType === 'OpenApi') {
      const {identifier: {organisation, name, version}, defaultNamespace, packageType, serviceBasePath} = (this.fileSystemPackageConfig.loader as OpenApiPackageLoaderSpec)
      const uriSafeProjectId = `${organisation}:${name}:${version}`
      this.schemaService.uploadProject(
        uriSafeProjectId,
        {
          format: packageType,
          defaultNamespace,
          serviceBasePath
        },
        this.filePayload
      ).subscribe({
        next: (result) => this.creationSuccess(result),
        error: (error) => this.creationError(error)
      });
    } else if (this.fileSystemPackageConfig.loader.packageType === 'Avro') {
      const {identifier: {organisation, name, version}, packageType} = (this.fileSystemPackageConfig.loader as AvroPackageLoaderSpec)
      const uriSafeProjectId = `${organisation}:${name}:${version}`
      this.schemaService.uploadProject(
        uriSafeProjectId,
        {
          format: packageType,
        },
        this.filePayload
      ).subscribe({
        next: (result) => this.creationSuccess(result),
        error: (error) => this.creationError(error)
      });
    }
  }

  private creationSuccess(result) {
    this.localFileAdded.emit();
    this.working = false;
    if (!this.isOnboardingMode) {
      this.alertsService.open(
        'The local disk repository was added successfully',
        { appearance: 'success' }
      ).subscribe()
      this.router.navigate(['projects']);
    } else {
      this.saveResultMessage = {
        message: 'The local disk repository was added successfully',
        severity: 'SUCCESS'
      };
    }
    this.changeDetector.markForCheck();
  }

  private creationError(error) {
    console.log(JSON.stringify(error));
    this.working = false;
    this.saveResultMessage = {
      message: `There was a problem adding the local disk repository: ${error.error.message}`,
      severity: 'ERROR'
    };
    this.changeDetector.markForCheck();
  }

}

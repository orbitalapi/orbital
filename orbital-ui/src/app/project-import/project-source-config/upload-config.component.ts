import {TuiButtonLoading, TuiTab, TuiTabsHorizontal} from '@taiga-ui/kit';
import {CommonModule} from '@angular/common';
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {TuiInputModule} from '@taiga-ui/legacy';
import {UiCustomisations} from '../../../environments/ui-customisations';
import {DataExplorerModule} from '../../data-explorer/data-explorer.module';
import {PackageIdentifierInputComponent} from '../../package-identifier-input/package-identifier-input.component';
import {FileSystemPackageSpec, TaxiPackageLoaderSpec} from '../project-import.models';
import {Message} from 'src/app/services/schema';
import {SchemaImporterService} from 'src/app/project-import/schema-importer.service';
import {TuiAlertService, TuiNotification, TuiButton, TuiGroup} from '@taiga-ui/core';
import {Router} from '@angular/router';
import {FilePathOrUploadComponent} from './file-path-or-upload.component';

@Component({
  selector: 'app-upload-config',
  standalone: true,
  styleUrls: ['./upload-config.component.scss'],
  template: `
    <div class='form-header-text' *ngIf="editable">
      <tui-notification appearance='info'>
        This creates a project on your server. For production use cases we recommend deploying Git-backed projects.
      </tui-notification>
    </div>
    <form #fileForm='ngForm'>
      <div class='form-container'>
        <div class='form-body'>
          <div class="form-row">
            <div class="form-item-description-container">
              <h3>Project source</h3>
              <div class="help-text">
                Select a zipped Taxi project. Should be a single zip file containing your taxi project, including the taxi.conf file
              </div>
            </div>
            <div class="form-element">
              <app-file-path-or-upload
                mode="upload"
                readContentsAs="bytes"
                [filesAccepted]="['.zip']"
                (fileChanged)="onFileChanged($event)"
                ngModelGroup="fileUpload"
                uploadLabel="choose a .zip file"
                fileExtensionErrorLabel="Invalid file extension. Allowed extension is: .zip"
              >
              </app-file-path-or-upload>
            </div>
          </div>
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
  imports: [
    CommonModule,
    FormsModule,
    TuiNotification,
    TuiGroup,
    TuiButton,
    TuiButtonLoading,
    PackageIdentifierInputComponent,
    DataExplorerModule,
    TuiInputModule,
    TuiTab,
    TuiTabsHorizontal,
    ReactiveFormsModule,
    FilePathOrUploadComponent,

  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UploadConfigComponent {
  @Input()
  fileSystemPackageConfig: FileSystemPackageSpec = new FileSystemPackageSpec();
  @Input()
  editable: boolean = true;
  @Input()
  isOnboardingMode: boolean;
  @Output()
  goBackOnboarding: EventEmitter<void> = new EventEmitter();
  @Output()
  projectUploaded: EventEmitter<void> = new EventEmitter();

  working = false;
  saveResultMessage: Message;
  protected readonly UiCustomisations = UiCustomisations;
  private filePayload: string;

  constructor(
    private changeDetector: ChangeDetectorRef,
    private schemaService: SchemaImporterService,
    private alertsService: TuiAlertService,
    private router: Router,
  ) {
    this.fileSystemPackageConfig.loader = new TaxiPackageLoaderSpec();
  }

  onFileChanged($event: string) {
    this.filePayload = $event;
  }

  doCreate() {
    this.working = true;
    this.saveResultMessage = null;
    this.schemaService.uploadProject('unknown', {format: 'Taxi'}, this.filePayload)
      .subscribe({
        next: (result) => this.creationSuccess(result),
        error: (error) => this.creationError(error),
      });
  }

  private creationSuccess(result) {
    this.projectUploaded.emit();
    this.working = false;
    if (!this.isOnboardingMode) {
      this.alertsService.open(
        'The project was uploaded successfully',
        {appearance: 'success'},
      ).subscribe();
      this.router.navigate(['projects']);
    } else {
      this.saveResultMessage = {
        message: 'The project was uploaded successfully',
        severity: 'SUCCESS',
      };
    }
    this.changeDetector.markForCheck();
  }

  private creationError(error) {
    console.log(JSON.stringify(error));
    this.working = false;
    this.saveResultMessage = {
      message: `There was a problem uploading the project: ${error.error?.message ?? error.message ?? 'An error occurred'}`,
      severity: 'ERROR',
    };
    this.changeDetector.markForCheck();
  }
}

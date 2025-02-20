import { TuiInputModule } from "@taiga-ui/legacy";
import {CommonModule} from '@angular/common';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  Input, model,
  Output
} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ControlContainer, FormsModule, NgModelGroup} from '@angular/forms';
import { TuiCheckbox } from '@taiga-ui/kit';
import {of} from 'rxjs';
import {catchError, debounceTime, switchMap} from 'rxjs/operators';
import {UiCustomisations} from '../../../environments/ui-customisations';
import {PackageIdentifierInputComponent} from '../../package-identifier-input/package-identifier-input.component';
import {FileSystemPackageSpec} from '../project-import.models';
import {isNullOrUndefined} from 'src/app/utils/utils';
import {FileRepositoryTestResponse, SchemaImporterService} from 'src/app/project-import/schema-importer.service';
import { TuiNotification, TuiLoader, TuiGroup, TuiButton } from '@taiga-ui/core';
import {AddProjectWorkflowType} from "./file-config.component";
import {FilePathOrUploadComponent} from "./file-path-or-upload.component";
import {detectSeperator, joinWithSeparator} from "../../utils/files";

@Component({
  selector: 'app-taxi-package-config',
  template: `
    <div class='form-row'>
      <div class='form-item-description-container'>
        <h3>Project path</h3>
        <div class='help-text'>{{ pathLabel }}</div>
      </div>
      <div class='form-element'>
        <app-file-path-or-upload *ngIf="editable && workflowType === 'fileUpload'"
                                 [mode]="'fileUpload'"
                                 readContentsAs="bytes"
                                 [filesAccepted]="['.zip']"
                                 (fileChanged)="fileChange.emit($event)"
                                 ngModelGroup="taxiFileUpload"
                                 uploadLabel="choose a .zip file"
                                 fileExtensionErrorLabel="Invalid file extension. Allowed extension is: .zip"
        >
        </app-file-path-or-upload>
        <app-file-path-or-upload *ngIf="!editable || workflowType !== 'fileUpload'"
                                 [editable]="editable"
                                 [mode]="workflowType"
                                 [filesAccepted]="['']"
                                 [path]="fileSystemPackageConfig?.path"
                                 (pathChange)="filePathUpdated($event)"
                                 ngModelGroup="taxiFilePath"
                                 fileExtensionErrorLabel="Set a path to a directory containing a taxi.conf file"
        >
        </app-file-path-or-upload>
        <div class='row' *ngIf="workflowType === 'pathToFile'">
          <div style='flex-grow: 1;'>
            <div style='display: flex; margin-top: 0.5rem'>
              <tui-loader [showLoader]='true' size='s'
                          *ngIf='editable && !filePathTestResult && fileSystemPackageConfig.path'
                          [textContent]="'Checking for a taxi project file at ' + expectedTaxiConfLocation"
              ></tui-loader>
              <tui-notification size="m" *ngIf='filePathTestResult?.exists' appearance='success'>
                Great - we've found project {{ filePathTestResult.identifier.id }} there
              </tui-notification>
              <div style='display: flex; width: 100%; align-items: center;'
                   *ngIf='filePathTestResult && !filePathTestResult.exists && !filePathTestResult.errorMessage'>
                <tui-notification size="m" style='flex-grow: 1'
                                  appearance='info'>
                  Can't find a project at {{ expectedTaxiConfLocation }}
                </tui-notification>
                <button tuiButton size='s' appearance='outline' style='margin-left: 1rem'
                        (click)='creatingNewProject() ? cancelCreateNewProject() : createNewProject()'>
                  {{ creatingNewProject() ? 'Cancel creation' : 'Create new project...' }}
                </button>
              </div>
              <div style='display: flex; width: 100%; align-items: center;'
                   *ngIf='filePathTestResult && filePathTestResult.errorMessage'>
                <tui-notification size="m" style='flex-grow: 1'
                                  appearance='error'>
                  {{ filePathTestResult.errorMessage }}
                </tui-notification>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class='form-row' *ngIf='creatingNewProject()'>
      <div class='form-item-description-container'>
        <h3>Package identifier</h3>
        <div class='help-text'>
          All schemas in {{ UiCustomisations.productName }} need a Package Identifier - similar
          to npm or maven
          co-ordinates
        </div>
      </div>
      <div class='form-element'>
        <div tuiGroup>
          <app-package-identifier-input
            ngModelGroup="taxiCoordinates"
            [(packageIdentifier)]="fileSystemPackageConfig.newProjectIdentifier"
            [editable]="editable"></app-package-identifier-input>
        </div>
      </div>
    </div>
    <div class='form-row'>
      <div class='form-item-description-container'>
        <h3>Enable edits</h3>
        <div class='help-text'>
          <p>
            If enabled, edits can be made through the {{ UiCustomisations.productName }} UI
          </p>
        </div>
      </div>
      <div class='form-element'>
        <input
          tuiCheckbox
          type="checkbox" [(ngModel)]='fileSystemPackageConfig.isEditable' name='editable'
          required [disabled]='!editable' size="s"/>
      </div>
    </div>
  `,
  styleUrls: ['./file-config.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TuiInputModule,
    TuiLoader,
    TuiNotification,
    PackageIdentifierInputComponent,
    TuiCheckbox,
    TuiButton,
    TuiGroup,
    FilePathOrUploadComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [{provide: ControlContainer, useExisting: NgModelGroup}],
})
export class TaxiPackageConfigComponent {
  @Input()
  workflowType: AddProjectWorkflowType = 'pathToFile';

  @Input()
  fileSystemPackageConfig: FileSystemPackageSpec;
  @Input()
  editable: boolean = true;
  @Input()
  filePathTestResult: FileRepositoryTestResponse;

  @Output()
  fileChange = new EventEmitter<string>();

  creatingNewProject = model<boolean>(false)

  private filePathChanged$ = new EventEmitter<string>();

  constructor(private changeDetector: ChangeDetectorRef,
              private schemaService: SchemaImporterService,
              private destroyRef: DestroyRef
  ) {
    this.filePathChanged$
      .pipe(
        takeUntilDestroyed(),
        debounceTime(500),
        // distinctUntilChanged(),
        switchMap((path: string) => {
          return schemaService.testFileConnection({path}).pipe(
            catchError((err: HttpErrorResponse) => of({
              exists: false,
              path: '',
              identifier: null,
              errorMessage: err.error?.message ? err.error.message : 'An error occurred checking the path'
            } as FileRepositoryTestResponse))
          );
        })
      )
      .subscribe(result => {
        this.filePathTestResult = result;
        if (result.exists) {
          this.creatingNewProject.set(false);
          this.fileSystemPackageConfig.newProjectIdentifier = null;
        }
        this.changeDetector.markForCheck();
      });
  }

  get expectedTaxiConfLocation(): string | null {
    if (isNullOrUndefined(this.fileSystemPackageConfig.path)) {
      return null;
    } else {
      const separator = detectSeperator(this.fileSystemPackageConfig.path)
      return joinWithSeparator( this.fileSystemPackageConfig.path, 'taxi.conf', separator)
    }
  }

  filePathUpdated(value: string) {
    this.fileSystemPackageConfig.path = value;
    this.filePathChanged$.emit(value);
    this.filePathTestResult = null;
  }

  createNewProject() {
    this.creatingNewProject.set(true);
    this.fileSystemPackageConfig.newProjectIdentifier = {
      name: null,
      organisation: null,
      version: '1.0.0',
      id: null,
      unversionedId: null

    };
    this.changeDetector.markForCheck();
  }

  get pathLabel(): string {
    if (!this.editable) {
      return '';
    }
    switch (this.workflowType) {
      case 'fileUpload':
        return 'Select a zipped Taxi project. Should be a single zip file containing your taxi project, including the taxi.conf file';
      case "pathToFile":
        return 'The path on the server to the taxi.conf file';
      case "git":
        return 'Path from the root of the git repository to the taxi.conf file';
    }
  }

  // The path on the server containing the <code>taxi.conf</code> file

  cancelCreateNewProject() {
    this.creatingNewProject.set(false)
    delete this.fileSystemPackageConfig.newProjectIdentifier
    this.changeDetector.markForCheck();
  }

  protected readonly UiCustomisations = UiCustomisations;
}

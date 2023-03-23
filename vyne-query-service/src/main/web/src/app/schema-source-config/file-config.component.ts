import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input } from '@angular/core';
import { projectTypeToString } from 'src/app/schema-source-config/git-config.component';
import {
  FileSystemPackageSpec,
  LoadablePackageType,
  OpenApiPackageLoaderSpec,
  TaxiPackageLoaderSpec
} from 'src/app/schema-importer/schema-importer.models';
import { isNullOrUndefined } from 'util';
import { Message } from 'src/app/services/schema';
import { FileRepositoryTestResponse, SchemaImporterService } from 'src/app/schema-importer/schema-importer.service';
import { catchError, debounceTime, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-file-config',
  template: `
    <div class='form-header-text'>
      <p>Read projects directly from the local machine.</p>
      <tui-notification status='warning'>Disk based projects are great for getting started and dev-local
        experiments,
        however you should consider storing your project in a git repository in production
      </tui-notification>
    </div>
    <form #gitForm='ngForm'>
      <div class='form-container'>
        <div class='form-body'>
          <div class='form-row'>
            <div class='form-item-description-container'>
              <h3>Project type</h3>
              <div class='help-text'>
                Git repositories can contain full Taxi projects, or individual API specs.
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
                  <button tuiOption value='OpenApi'>{{ stringifyProjectType('OpenApi')}}</button>
                </tui-data-list>
              </tui-select>
            </div>
          </div>
          <app-open-api-package-config *ngIf="fileSystemPackageConfig.loader.packageType ==='OpenApi'"
                                       [openApiPackageSpec]='openApiPackageSpec'
                                       [(path)]='fileSystemPackageConfig.path'
          ></app-open-api-package-config>
          <ng-container *ngIf="fileSystemPackageConfig.loader.packageType === 'Taxi'">
            <div class='form-row'>
              <div class='form-item-description-container'>
                <h3>Project path</h3>
                <div class='help-text'>
                  <p>
                    Specify the path to the directory containing a <code>taxi.conf</code> file.
                  </p>
                </div>
              </div>
              <div class='form-element'>
                <div class='row'>
                  <div style='flex-grow: 1;'>
                    <tui-input [ngModel]='fileSystemPackageConfig.path' class='flex-grow'
                               name='pathToTaxi' required [readOnly]='!editable'
                               (ngModelChange)='filePathUpdated($event)'
                    >
                      Path
                    </tui-input>
                    <div style='display: flex; margin-top: 0.5rem'>
                      <tui-loader [showLoader]='true' size='s'
                                  *ngIf='editable && !filePathTestResult && fileSystemPackageConfig.path'
                                  [textContent]="'Checking for a taxi project file at ' + expectedTaxiConfLocation"></tui-loader>


                      <tui-notification *ngIf='filePathTestResult?.exists' status='success'>
                        Great - we've found project {{ filePathTestResult.identifier.id}} there
                      </tui-notification>
                      <div style='display: flex; width: 100%; align-items: center;'
                           *ngIf='filePathTestResult && !filePathTestResult.exists && !filePathTestResult.errorMessage'>
                        <tui-notification style='flex-grow: 1'
                                          status='info'>
                          Can't find a project at {{ expectedTaxiConfLocation }}
                        </tui-notification>
                        <button tuiButton size='s' appearance='outline' style='margin-left: 1rem'
                                (click)='createNewProject()'>Create new project
                        </button>
                      </div>
                      <div style='display: flex; width: 100%; align-items: center;'
                           *ngIf='filePathTestResult && filePathTestResult.errorMessage'>
                        <tui-notification style='flex-grow: 1'
                                          status='error'>
                          {{ filePathTestResult.errorMessage }}
                        </tui-notification>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class='form-row' *ngIf='creatingNewProject'>
              <div class='form-item-description-container'>
                <h3>Package identifier</h3>
                <div class='help-text'>
                  All schemas in Orbital need a Package Identifier - similar to npm or maven
                  co-ordinates
                </div>
              </div>
              <div class='form-element'>
                <div tuiGroup>
                  <tui-input [(ngModel)]='fileSystemPackageConfig.newProjectIdentifier.organisation' required
                             name='openApiPackageOrg'>
                    Organisation
                  </tui-input>
                  <tui-input [(ngModel)]='fileSystemPackageConfig.newProjectIdentifier.name' required
                             name='openApiPackageName'>
                    Name
                  </tui-input>
                  <tui-input [(ngModel)]='fileSystemPackageConfig.newProjectIdentifier.version' required
                             name='openApiPackageVersion'>
                    Version
                  </tui-input>
                </div>
              </div>
            </div>
            <div class='form-row'>
              <div class='form-item-description-container'>
                <h3>Enable edits</h3>
                <div class='help-text'>
                  <p>
                    If enabled, edits can be made through the Orbital UI
                  </p>
                </div>
              </div>
              <div class='form-element'>
                <tui-checkbox [(ngModel)]='fileSystemPackageConfig.isEditable' name='editable'
                              required [readOnly]='!editable'></tui-checkbox>
              </div>
            </div>
          </ng-container>
        </div>
      </div>
    </form>
    <div *ngIf='editable' class='form-button-bar'>
      <button tuiButton [showLoader]='working' [size]="'m'" (click)='doCreate()' [disabled]='gitForm.invalid'>Create
      </button>
    </div>
    <tui-notification [status]='saveResultMessage.level.toLowerCase()' *ngIf='saveResultMessage'>
      {{ saveResultMessage.message }}
    </tui-notification>
  `,
  styleUrls: ['./file-config.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FileConfigComponent {

  readonly stringifyProjectType = projectTypeToString;
  @Input()
  fileSystemPackageConfig: FileSystemPackageSpec = new FileSystemPackageSpec();
  @Input()
  editable: boolean = true;
  working = false;
  saveResultMessage: Message;

  creatingNewProject: boolean = false;

  filePathTestResult: FileRepositoryTestResponse;
  private filePathChanged$ = new EventEmitter<string>();

  constructor(private changeDetector: ChangeDetectorRef, private schemaService: SchemaImporterService) {
    this.filePathChanged$
      .pipe(
        debounceTime(500),
        // distinctUntilChanged(),
        switchMap((path: string) => {
            return schemaService.testFileConnection({ path }).pipe(
              catchError(err => of({
                exists: false,
                path: '',
                identifier: null,
                errorMessage: 'An error occurred checking the path'
              } as FileRepositoryTestResponse))
            );
          }
        )).subscribe(result => {
      this.filePathTestResult = result;
      if (result.exists) {
        this.creatingNewProject = false;
        this.fileSystemPackageConfig.newProjectIdentifier = null;
      }
      this.changeDetector.markForCheck();
    });

  }

  get expectedTaxiConfLocation(): string | null {
    if (isNullOrUndefined(this.fileSystemPackageConfig.path)) {
      return null;
    } else {
      const seperator = this.fileSystemPackageConfig.path.endsWith('/') ? '' : '/';
      return this.fileSystemPackageConfig.path + seperator + 'taxi.conf';
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
    switch (projectType) {
      case 'Taxi':
        this.fileSystemPackageConfig.loader = new TaxiPackageLoaderSpec();
        break;
      case 'OpenApi':
        this.fileSystemPackageConfig.loader = new OpenApiPackageLoaderSpec();
        break;
    }
    this.changeDetector.markForCheck();
  }

  doCreate() {
    console.log(JSON.stringify(this.fileSystemPackageConfig, null, 2));
    this.working = true;
    this.saveResultMessage = null;
    this.schemaService.addNewFileRepository(this.fileSystemPackageConfig)
      .subscribe(result => {
          this.working = false;
          this.saveResultMessage = {
            message: 'The local disk repository was added successfully',
            level: 'SUCCESS'
          };
          this.changeDetector.markForCheck();
        },
        error => {
          console.log(JSON.stringify(error));
          this.working = false;
          this.saveResultMessage = {
            message: `There was a problem adding the local disk repository: ${error.error.message}`,
            level: 'ERROR'
          };
          this.changeDetector.markForCheck();
        });
  }

  onFileSelected($event: any) {
    console.log($event);
  }


  filePathUpdated(value: string) {
    this.fileSystemPackageConfig.path = value;
    this.filePathChanged$.emit(value);
    this.filePathTestResult = null;
  }

  createNewProject() {
    this.creatingNewProject = true;
    this.fileSystemPackageConfig.newProjectIdentifier = {
      name: null,
      organisation: null,
      version: null,
      id: null,
      unversionedId: null

    };
    this.changeDetector.markForCheck();
  }
}

import { TuiInputModule } from "@taiga-ui/legacy";
import {CommonModule} from '@angular/common';
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, EventEmitter, Input} from '@angular/core';
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

@Component({
  selector: 'app-taxi-package-config',
  template: `
    <div class='form-row'>
      <div class='form-item-description-container'>
        <h3>Project path</h3>
        <div class='help-text'>
          <p>
            The path on the server containing the <code>taxi.conf</code> file
          </p>
        </div>
      </div>
      <div class='form-element'>
        <div class='row'>
          <div style='flex-grow: 1;'>
            <tui-input [ngModel]='fileSystemPackageConfig.path' class='flex-grow'
                       name='pathToTaxi' required [readOnly]='!editable'
                       (ngModelChange)='filePathUpdated($event)'>
              Path
            </tui-input>
            <tui-notification size="m" appearance='neutral' class="tui-space_top-2" *ngIf="editable">
              Using Docker? Enter paths relative to your mounted volume (e.g., /opt/service/workspace )
            </tui-notification>
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
                        (click)='creatingNewProject ? cancelCreateNewProject() : createNewProject()'>
                  {{ creatingNewProject ? 'Cancel creation' : 'Create new project...'}}
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
    <div class='form-row' *ngIf='creatingNewProject'>
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
    TuiGroup
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [{provide: ControlContainer, useExisting: NgModelGroup}],
})
export class TaxiPackageConfigComponent {
  @Input()
  fileSystemPackageConfig: FileSystemPackageSpec;
  @Input()
  editable: boolean = true;
  @Input()
  filePathTestResult: FileRepositoryTestResponse;

  creatingNewProject: boolean;
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
      let separator;
      if (this.fileSystemPackageConfig.path.includes("/")) {
        separator = this.fileSystemPackageConfig.path.endsWith('/') ? '' : '/';
      } else {
        separator = this.fileSystemPackageConfig.path.endsWith('\\') ? '' : '\\';
      }
      return this.fileSystemPackageConfig.path + separator + 'taxi.conf';
    }
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
      version: '1.0.0',
      id: null,
      unversionedId: null

    };
    this.changeDetector.markForCheck();
  }

  cancelCreateNewProject() {
    this.creatingNewProject = false
    delete this.fileSystemPackageConfig.newProjectIdentifier
    this.changeDetector.markForCheck();
  }

  protected readonly UiCustomisations = UiCustomisations;
}

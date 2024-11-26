import {TuiButtonLoading} from "@taiga-ui/kit";
import {CommonModule} from '@angular/common';
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {UiCustomisations} from '../../../environments/ui-customisations';
import {PackageIdentifierInputComponent} from '../../package-identifier-input/package-identifier-input.component';
import {FileSystemPackageSpec, TaxiPackageLoaderSpec} from '../project-import.models';
import {Message} from 'src/app/services/schema';
import {SchemaImporterService} from 'src/app/project-import/schema-importer.service';
import {TuiAlertService, TuiNotification, TuiButton, TuiGroup} from '@taiga-ui/core';
import {Router} from "@angular/router";

@Component({
  selector: 'app-create-new-config',
  standalone: true,
  styleUrls: ['./create-new-config.component.scss'],
  template: `
    <div class='form-header-text' *ngIf="editable">
      <p>Create a new server-hosted project.</p>
      <tui-notification appearance='info'>
        Server hosted projects are great for getting started, though for production use cases we recommend deploying
        Git-backed projects
      </tui-notification>
    </div>
    <form #fileForm='ngForm'>
      <div class='form-container'>
        <div class='form-body'>
          <div class='form-row'>
            <div class='form-item-description-container'>
              <h3>Package identifier</h3>
              <div class='help-text'>
                All projects in {{ UiCustomisations.productName }} need an identifier,
                similar to package identifiers in npm and NuGet, Maven coordinates, or Python package specifiers.
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

  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CreateNewConfigComponent {
  @Input()
  fileSystemPackageConfig: FileSystemPackageSpec = new FileSystemPackageSpec();
  @Input()
  editable: boolean = true;
  @Input()
  isOnboardingMode: boolean;
  @Output()
  goBackOnboarding: EventEmitter<void> = new EventEmitter();
  @Output()
  newProjectCreated: EventEmitter<void> = new EventEmitter()

  working = false;
  saveResultMessage: Message;

  constructor(
    private changeDetector: ChangeDetectorRef,
    private schemaService: SchemaImporterService,
    private alertsService: TuiAlertService,
    private router: Router
  ) {
    this.fileSystemPackageConfig.loader = new TaxiPackageLoaderSpec();
    this.fileSystemPackageConfig.newProjectIdentifier = {
      name: null,
      organisation: null,
      version: '1.0.0',
      id: null,
      unversionedId: null
    };
  }

  doCreate() {
    this.working = true;
    this.saveResultMessage = null;
    this.schemaService.createEmptyProject({
      newProjectIdentifier: this.fileSystemPackageConfig.newProjectIdentifier
    })
      .subscribe({
        next: (result) => this.creationSuccess(result),
        error: (error) => this.creationError(error)
      });
  }

  private creationSuccess(result) {
    this.newProjectCreated.emit();
    this.working = false;
    if (!this.isOnboardingMode) {
      this.alertsService.open(
        'The project was created successfully',
        {appearance: 'success'}
      ).subscribe()
      this.router.navigate(['projects']);
    } else {
      this.saveResultMessage = {
        message: 'The project was created successfully',
        severity: 'SUCCESS'
      };
    }
    this.changeDetector.markForCheck();
  }

  private creationError(error) {
    console.log(JSON.stringify(error));
    this.working = false;
    this.saveResultMessage = {
      message: `There was a problem creating the new project: ${error.error?.message ?? error.message ?? 'An error occurred'}`,
      severity: 'ERROR'
    };
    this.changeDetector.markForCheck();
  }

  protected readonly UiCustomisations = UiCustomisations;
}

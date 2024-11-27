import {TuiBlock, TuiButtonLoading, TuiCheckbox} from "@taiga-ui/kit";
import {CommonModule, NgIf} from '@angular/common';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges
} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {TuiButton, TuiNotification} from '@taiga-ui/core';
import {Observable} from 'rxjs/internal/Observable';
import {UiCustomisations} from '../../../../environments/ui-customisations';
import {ConvertSchemaEvent} from '../../../data-source-import/data-source-import.models';
import {PackagesService, SourcePackageDescription} from '../../../package-viewer/packages.service';
import {CreateOrReplaceSource, SchemaImporterService} from '../../../project-import/schema-importer.service';
import {ProjectSelectorModule} from '../../../project-selector/project-selector.module';
import {SchemaSubmissionResult} from '../../../services/types.service';

@Component({
  selector: 'app-map-auth-token-step',
  standalone: true,
  imports: [
    CommonModule, ProjectSelectorModule, TuiButton, TuiBlock, TuiCheckbox, FormsModule,
    TuiNotification, TuiButtonLoading, NgIf],
  template: `
    <h4>Map your authentication token</h4>
    <p>
      Your authentication service provides user details through to {{ UiCustomisations.productName }}, which can be used
      inside
      access policies.
    </p>
    <p>
      To make these available, you need to define a model in your schema that inherits from <code>{{UiCustomisations.packageNamespace}}.auth.AuthClaims</code>,
      which contains your user credentials.
    </p>
    <app-project-selector
      prompt="Select a project to add the policy to"
      [packages]="packages$ | async"
      [startOpened]="true"
      [(ngModel)]="selectedProject"
      (ngModelChange)="projectSelected.emit($event)"
      class="project-selector"
    ></app-project-selector>

    <p [class.is-readonly]="!selectedProject">
      Select the properties you want included in your auth claims object. These properties can be used in your policies
    </p>

    <div class="checkbox-container" *ngIf="claims" [class.is-readonly]="!selectedProject">

      <label *ngFor="let checkbox of checkboxes; trackBy: key"
             class="checkbox" tuiBlock
             contenAlight="right"
      >
        <div class="row">
          <div class="key">{{ checkbox.key }}</div>
          <div class="value">{{ checkbox.value }}</div>
        </div>
        <input tuiCheckbox type="checkbox" [(ngModel)]="checkbox.checked"/>
      </label>
      <tui-notification appearance="error" *ngIf="schemaConversionError"
                        class="notification-error">
        <div>{{ schemaConversionError }}</div>
        <button tuiIconButton iconStart="@tui.x" (click)="schemaConversionError = ''"></button>
      </tui-notification>
    </div>
    <div class="form-button-bar">
      <button tuiButton [loading]="working" [size]="'m'" (click)="onSubmit()" [disabled]="!isFormValid()">Configure
      </button>
    </div>
  `,
  styleUrls: ['./map-auth-token-step.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MapAuthTokenStepComponent implements OnChanges {
  @Input()
  claims!: { [p: string]: any } | undefined;

  @Output()
  schemaSubmissionResultReceived: EventEmitter<CreateOrReplaceSource> = new EventEmitter<CreateOrReplaceSource>();

  @Output()
  projectSelected = new EventEmitter<SourcePackageDescription>();

  checkboxes: { key: string, value: string, checked: boolean }[];
  working: boolean;
  selectedProject: SourcePackageDescription;
  packages$: Observable<SourcePackageDescription[]>
  schemaConversionError: string;

  protected readonly UiCustomisations = UiCustomisations;


  constructor(
    private packagesService: PackagesService,
    private changeDetectorRef: ChangeDetectorRef,
    private schemaImporterService: SchemaImporterService,
  ) {
    this.packages$ = packagesService.listPackages();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.claims?.currentValue) {
      const claims = changes.claims.currentValue;
      this.checkboxes = Object.keys(claims).sort().map(key => {
        const val = claims[key];
        const isObject = typeof val === 'object' && !Array.isArray(val) && val !== null;
        let _value: string;
        if (Array.isArray(val)) {
          _value = '[' + val.join(", ") + ']'
        } else if (isObject) {
          _value = JSON.stringify(val)
        } else {
          _value = val;
        }
        return {key, value: _value, checked: false}
      })
    }
  }

  onSubmit() {
    const selectedCheckboxes = this.checkboxes.filter(checkbox => checkbox.checked).map(checkbox => checkbox.key);
    console.log(this.selectedProject, selectedCheckboxes);

    this.working = true;
    const convertSchemaEvent: ConvertSchemaEvent = {
      schemaType: 'map',
      options: {
        map: this.claims,
        // The user could probably change this
        // TODO: do we need an input field for this?
        createdTypeName: 'UserCredentials',
        // This must be the below value
        inheritedTypeName: `${UiCustomisations.packageNamespace}.auth.AuthClaims`,
        // TODO: is this correct value for fieldsToInclude?
        fieldsToInclude: selectedCheckboxes
      },
      packageIdentifier: this.selectedProject.identifier
    }
    this.schemaImporterService.convertSchema(convertSchemaEvent).subscribe({
      next: (result: SchemaSubmissionResult<CreateOrReplaceSource>) => {
        console.log(result);
        this.schemaSubmissionResultReceived.emit(result.pendingEdits[0])
        this.working = false;
        this.changeDetectorRef.markForCheck();
      },
      error: (error) => {
        console.error(JSON.stringify(error, null, 2));
        this.schemaConversionError = error.error?.message || error.message || 'An error occurred';
        this.working = false;
        this.changeDetectorRef.markForCheck();
      }
    });
  }

  isFormValid() {
    return this.checkboxes.filter(checkbox => checkbox.checked).length > 0 && this.selectedProject;
  }
}

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {TuiHintModule, TuiNotificationModule, tuiNotificationOptionsProvider} from '@taiga-ui/core';
import {TuiInputModule} from '@taiga-ui/kit';
import {OpenApiPackageLoaderSpec} from 'src/app/project-import/project-import.models';
import {ControlContainer, FormsModule, NgModelGroup, ReactiveFormsModule} from '@angular/forms';
import {UiCustomisations} from '../../../environments/ui-customisations';
import {PackageIdentifierInputComponent} from '../../package-identifier-input/package-identifier-input.component';
import {FilePathOrUploadComponent} from './file-path-or-upload.component';

@Component({
  selector: 'app-open-api-package-config',
  standalone: true,
  imports: [
    FilePathOrUploadComponent,
    PackageIdentifierInputComponent,
    TuiInputModule,
    FormsModule,
    TuiNotificationModule,
    TuiHintModule,
  ],
  providers: [
    tuiNotificationOptionsProvider({
      icon: 'tuiIconHelpCircle',
      status: 'info',
    }),
  ],
  viewProviders: [{ provide: ControlContainer, useExisting: NgModelGroup }],
  template: `
    <tui-notification status="info" class="open-api-notification" [tuiHint]="tooltip" tuiHintAppearance="onDark" tuiHintShowDelay="100">
      Understanding the difference between adding a project vs adding a data source
    </tui-notification>
    <ng-template #tooltip>
      <p>Using this approach, the OpenAPI spec remains as your source of truth, and you create links between an OpenAPI service and your models by embedding Taxi metadata directly within the spec.</p>
      <p>Use this when you'd prefer to work with OpenAPI over Taxi.</p>
      <p>Alternatively, you can add the OpenAPI spec as a data source, which will convert the spec to Taxi, and write it to your project.</p>
    </ng-template>
    <div class="form-row">
      <div class="form-item-description-container">
        <h3>OpenAPI spec file</h3>
        <div class="help-text">{{ pathLabel }}</div>
      </div>
      <div class="form-element">
        <app-file-path-or-upload
          [editable]="editable"
          [mode]="projectType === 'file' ? 'upload' : 'path'"
          [filesAccepted]="['.json', '.yaml']"
          [path]="path"
          (pathChanged)="onPathChanged($event)"
          (fileChanged)="fileChange.emit($event)"
          uploadLabel="choose a .json or .yaml file"
          fileExtensionErrorLabel="Invalid file extension. Allowed extensions are: .json and .yaml"
        >
        </app-file-path-or-upload>
      </div>
    </div>
    <div class="form-row">
      <div class="form-item-description-container">
        <h3>Package identifier</h3>
        <div class="help-text">
          All schemas in {{ UiCustomisations.productName }} need a Package Identifier - similar to npm or maven
          co-ordinates
        </div>
      </div>
      <div class="form-element">
        <app-package-identifier-input
          ngModelGroup="packageIdFormGroup"
          [(packageIdentifier)]="openApiPackageSpec.identifier"
          [editable]="editable"
          (defaultNamespaceChange)="openApiPackageSpec.defaultNamespace = $event"
        ></app-package-identifier-input>
      </div>
    </div>
    <div class="form-row">
      <div class="form-item-description-container">
        <h3>Service base path</h3>
        <div class="help-text">
          <p>If the OpenAPI spec doesn't provide a base path, then supply one here.</p>
          <p>eg: https://petstore.acme.com/</p>
        </div>
      </div>
      <div class="form-element">
        <tui-input [(ngModel)]="openApiPackageSpec.serviceBasePath" name="serviceBasePath" [readOnly]="!editable">
          Base path
        </tui-input>
      </div>
    </div>
    <div class="form-row">
      <div class="form-item-description-container">
        <h3>Default namespace</h3>
        <div class="help-text">
          When {{ UiCustomisations.productName }} imports the OpenAPI spec, it will generate services within this
          namespace
        </div>
      </div>
      <div class="form-element">
        <tui-input [(ngModel)]="openApiPackageSpec.defaultNamespace" name="defaultNamespace" [readOnly]="!editable">
          Default namespace
        </tui-input>
      </div>
    </div>
  `,
  styleUrls: ['./open-api-package-config.component.scss']
})
export class OpenApiPackageConfigComponent {
  @Input()
  projectType: 'file' | 'git' = 'file';

  @Input()
  openApiPackageSpec: OpenApiPackageLoaderSpec;

  @Input()
  editable: boolean = true;

  @Input()
  path: string;

  @Output()
  pathChange = new EventEmitter<string>();

  @Output()
  fileChange = new EventEmitter<string>();

  get pathLabel(): string {
    if (this.projectType === 'file') {
      return 'Select your OpenAPI spec file'
    } else {
      return 'Path from the root of the git repository to the OpenAPI spec file';
    }
  }

  onPathChanged(value: string) {
    this.path = value;
    this.pathChange.emit(value);
  }

  protected readonly UiCustomisations = UiCustomisations;
}

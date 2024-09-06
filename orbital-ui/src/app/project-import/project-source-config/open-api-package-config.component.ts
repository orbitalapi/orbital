import {Component, EventEmitter, Input, Output} from '@angular/core';
import {TuiInputModule} from '@taiga-ui/kit';
import {OpenApiPackageLoaderSpec} from 'src/app/project-import/project-import.models';
import {ControlContainer, FormsModule, NgModelGroup, ReactiveFormsModule} from '@angular/forms';
import {UiCustomisations} from '../../../environments/ui-customisations';
import {PackageIdentifierInputComponent} from '../../package-identifier-input/package-identifier-input.component';
import {FilePathOrUploadComponent} from './file-path-or-upload.component';

@Component({
  selector: 'app-open-api-package-config',
  standalone: true,
  template: `
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
  styleUrls: ['./open-api-package-config.component.scss'],
  imports: [
    FilePathOrUploadComponent,
    PackageIdentifierInputComponent,
    TuiInputModule,
    FormsModule
  ],
  viewProviders: [{ provide: ControlContainer, useExisting: NgModelGroup }]
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

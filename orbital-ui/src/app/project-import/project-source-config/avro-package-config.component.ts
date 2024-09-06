import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ControlContainer, FormsModule, NgModelGroup} from '@angular/forms';
import {AvroPackageLoaderSpec} from "../project-import.models";
import {UiCustomisations} from "../../../environments/ui-customisations";
import {PackageIdentifierInputComponent} from "../../package-identifier-input/package-identifier-input.component";
import {FilePathOrUploadComponent} from './file-path-or-upload.component';

@Component({
  selector: 'app-avro-package-config',
  viewProviders: [{provide: ControlContainer, useExisting: NgModelGroup}],
  standalone: true,
  imports: [
    FilePathOrUploadComponent,
    PackageIdentifierInputComponent,
    FormsModule
  ],
  template: `
    <div class='form-row'>
      <div class='form-item-description-container'>
        <h3>Avro spec file</h3>
        <div class='help-text'>
          <p>{{ pathLabel }} </p>
        </div>
      </div>
      <div class='form-element'>
        <app-file-path-or-upload
          [editable]="editable"
          [mode]="projectType === 'file' ? 'upload' : 'path'"
          [filesAccepted]="['.avsc']"
          [path]="path"
          (pathChanged)="onPathChanged($event)"
          (fileChanged)="fileChange.emit($event)"
          uploadLabel="choose an .avsc file"
          fileExtensionErrorLabel="Invalid file extension. Allowed extension is: .avsc"
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
          [(packageIdentifier)]="packageSpec.identifier"
          [editable]="editable"
        ></app-package-identifier-input>
      </div>
    </div>
  `,
  styleUrl: './avro-package-config.component.scss'
})
export class AvroPackageConfigComponent {
  @Input()
  projectType: 'file' | 'git' = 'file';

  @Input()
  packageSpec: AvroPackageLoaderSpec

  @Input()
  editable: boolean = true;

  @Input()
  path: string;

  @Output()
  pathChange = new EventEmitter<string>();

  @Output()
  fileChange = new EventEmitter<string>();

  errorMessage: string;

  get pathLabel(): string {
    if (this.projectType === 'file') {
      return 'Select your Avro avsc spec file'
    } else {
      return 'Path from the root of the git repository to the Avro spec file';
    }
  }

  onPathChanged(value: string) {
    this.path = value;
    this.pathChange.emit(value);
  }

  protected readonly UiCustomisations = UiCustomisations;
}

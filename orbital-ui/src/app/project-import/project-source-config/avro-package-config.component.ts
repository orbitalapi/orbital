import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ControlContainer, NgModelGroup, ReactiveFormsModule} from '@angular/forms';
import {AvroPackageLoaderSpec} from "../project-import.models";
import {UiCustomisations} from "../../../environments/ui-customisations";
import {PackageIdentifierInputComponent} from "../../package-identifier-input/package-identifier-input.component";
import {CovalentCommonModule} from "@covalent/core/common";
import {NgIf} from "@angular/common";
import {TuiButtonModule, TuiErrorModule, TuiLoaderModule, TuiNotificationModule} from '@taiga-ui/core';
import {TuiInputModule} from "@taiga-ui/kit";
import {fileExtensionValidator} from './file-extension-validator';

@Component({
  selector: 'app-avro-package-config',
  viewProviders: [{provide: ControlContainer, useExisting: NgModelGroup}],
  standalone: true,
  imports: [
    PackageIdentifierInputComponent,
    CovalentCommonModule,
    NgIf,
    TuiButtonModule,
    TuiInputModule,
    TuiLoaderModule,
    TuiNotificationModule,
    ReactiveFormsModule,
    TuiErrorModule
  ],
  template: `
    <div class='form-row'>
      <div class='form-item-description-container'>
        <h3>Path to Avro spec file</h3>
        <div class='help-text'>
          <p>{{ pathLabel }} </p>
        </div>
      </div>
      <div class='form-element'>
        <div class='row'>
          <div style='flex-grow: 1;'>
            <tui-input [ngModel]='path' class='flex-grow' (ngModelChange)="onPathChanged($event)" (focusout)="validatePath()"
                       name='pathToTaxi' required [readOnly]='!editable'>
              Path
              <span class="tui-required"></span>
            </tui-input>
            <tui-error [error]="errorMessage"/>
          </div>
        </div>
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
  packageSpec: AvroPackageLoaderSpec

  @Input()
  editable: boolean = true;

  @Input()
  path: string;

  @Output()
  pathChange = new EventEmitter<string>();

  @Input()
  projectType: 'file' | 'git' = 'file';

  errorMessage: string;

  get pathLabel(): string {
    if (this.projectType === 'file') {
      return 'Specify the path to your Avro avsc spec file'
    } else {
      return 'Specify the path (from the root of the git repository) to the Avro avsc spec file';
    }
  }

  validatePath(): void {
    const validator = fileExtensionValidator(['avsc']);
    const validationResult = validator({ value: this.path } as any);
    if (validationResult && validationResult.invalidFileExtension) {
      this.errorMessage = 'Invalid file extension. Only *.avsc files are allowed.';
    } else {
      this.errorMessage = null;
    }
  }

  onPathChanged(value: string) {
    this.path = value;
    this.pathChange.emit(value);
  }

  protected readonly UiCustomisations = UiCustomisations;
}

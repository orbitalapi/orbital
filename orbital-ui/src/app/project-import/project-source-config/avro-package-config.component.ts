import {Component, EventEmitter, Input, Output} from '@angular/core';
import {AvroPackageLoaderSpec} from "../project-import.models";
import {UiCustomisations} from "../../../environments/ui-customisations";
import {PackageIdentifierInputComponent} from "../../package-identifier-input/package-identifier-input.component";
import {CovalentCommonModule} from "@covalent/core/common";
import {NgIf} from "@angular/common";
import {TuiButtonModule, TuiLoaderModule, TuiNotificationModule} from "@taiga-ui/core";
import {TuiInputModule} from "@taiga-ui/kit";

@Component({
  selector: 'app-avro-package-config',
  standalone: true,
  imports: [
    PackageIdentifierInputComponent,
    CovalentCommonModule,
    NgIf,
    TuiButtonModule,
    TuiInputModule,
    TuiLoaderModule,
    TuiNotificationModule
  ],
  template: `
    <div class='form-row'>
      <div class='form-item-description-container'>
        <h3>Project path</h3>
        <div class='help-text'>
          <p>{{ pathLabel }} </p>
        </div>
      </div>
      <div class='form-element'>
        <div class='row'>
          <div style='flex-grow: 1;'>
            <tui-input [ngModel]='path' class='flex-grow' (ngModelChange)="onPathChanged($event)"
                       name='pathToTaxi' required [readOnly]='!editable'>
              Path
            </tui-input>
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
        <app-package-identifier-input [(packageIdentifier)]="packageSpec.identifier"
                                      [editable]="editable"></app-package-identifier-input>
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


  get pathLabel(): string {
    if (this.projectType === 'file') {
      return 'Specify the path to your Avro avsc spec file'
    } else {
      return 'Specify the path (from the root of the git repository) to the Avro avsc spec file';
    }
  }


  onPathChanged(value: string) {
    this.path = value;
    this.pathChange.emit(value);
  }

  protected readonly UiCustomisations = UiCustomisations;
}

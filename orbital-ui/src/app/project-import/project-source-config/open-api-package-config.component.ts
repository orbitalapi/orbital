import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {OpenApiPackageLoaderSpec} from 'src/app/project-import/project-import.models';
import {NgControl, NgModel} from '@angular/forms';
import {UiCustomisations} from '../../../environments/ui-customisations';

@Component({
  selector: 'app-open-api-package-config',
  template: `
    <div class="form-row">
      <div class="form-item-description-container">
        <h3>Path to Open API spec</h3>
        <div class="help-text">{{pathLabel}}</div>
      </div>
      <div class="form-element">
        <tui-input [ngModel]="path" (ngModelChange)="onPathChanged($event)" required name="path" [readOnly]="!editable">
          Path
        </tui-input>
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
        <app-package-identifier-input [(packageIdentifier)]="openApiPackageSpec.identifier"
                                      [editable]="editable"
                                      (defaultNamespaceChange)="openApiPackageSpec.defaultNamespace = $event"></app-package-identifier-input>
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

  get pathLabel(): string {
    if (this.projectType === 'file') {
      return 'Specify the path to your Open API spec file'
    } else {
      return 'Specify the path (from the root of the git repository) to the OpenAPI spec file';
    }
  }

  @Input()
  openApiPackageSpec: OpenApiPackageLoaderSpec;

  @Input()
  editable: boolean = true;

  @Input()
  path: string;

  @Output()
  pathChange = new EventEmitter<string>();

  onPathChanged(value: string) {
    this.path = value;
    this.pathChange.emit(value);
  }

  protected readonly UiCustomisations = UiCustomisations;
}

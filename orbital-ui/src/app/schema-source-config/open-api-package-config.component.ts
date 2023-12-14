import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { OpenApiPackageLoaderSpec } from 'src/app/schema-importer/schema-importer.models';
import { NgControl, NgModel } from '@angular/forms';

@Component({
  selector: 'app-open-api-package-config',
  template: `
    <div class="form-row">
      <div class="form-item-description-container">
        <h3>Path to Open API spec</h3>
        <div class="help-text">
          Specify the path (from the root of the git repository) to the OpenAPI spec file
        </div>
      </div>
      <div class="form-element">
        <tui-input [ngModel]="path" (ngModelChange)="onPathChanged($event)" required name="path">
          Path
        </tui-input>
      </div>
    </div>
    <div class="form-row">
      <div class="form-item-description-container">
        <h3>Package identifier</h3>
        <div class="help-text">
          All schemas in Orbital need a Package Identifier - similar to npm or maven
          co-ordinates
        </div>
      </div>
      <div class="form-element">
        <div tuiGroup *ngIf="openApiPackageSpec">
          <tui-input [(ngModel)]="openApiPackageSpec.identifier.organisation" required name="openApiPackageOrg"
                     (ngModelChange)="updateDefaultNamespace()" validIdentifier #openApiPackageOrg="ngModel">
            Organisation
          </tui-input>
          <tui-input [(ngModel)]="openApiPackageSpec.identifier.name" required name="openApiPackageName"
                     (ngModelChange)="updateDefaultNamespace()" validIdentifier #openApiPackageName="ngModel">
            Name
          </tui-input>
          <tui-input [(ngModel)]="openApiPackageSpec.identifier.version" required name="openApiPackageVersion" semver
                     #openApiPackageVersion="ngModel">
            Version
          </tui-input>
        </div>
        <tui-notification class="validation-error"
                          *ngIf="openApiPackageOrg$ && openApiPackageOrg$.invalid && (openApiPackageOrg$.dirty || openApiPackageOrg$.touched)"
                          status="error">Organisation names must start with a letter, and only contain letters,
          underscores, hyphens or numbers
        </tui-notification>
        <tui-notification class="validation-error"
                          *ngIf="openApiPackageOrg$ && openApiPackageName$.invalid && (openApiPackageName$.dirty || openApiPackageName$.touched)"
                          status="error">Package names must start with a letter, and only contain letters, underscores,
          hyphens or numbers
        </tui-notification>
        <tui-notification class="validation-error"
                          *ngIf="openApiPackageOrg$ && openApiPackageVersion$.invalid && (openApiPackageVersion$.dirty || openApiPackageVersion$.touched)"
                          status="error">Versions need to follow the convention of 0.0.0 (eg., 1.0.3)
        </tui-notification>
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
        <tui-input [(ngModel)]="openApiPackageSpec.serviceBasePath" name="serviceBasePath">
          Base path
        </tui-input>
      </div>
    </div>
    <div class="form-row">
      <div class="form-item-description-container">
        <h3>Default namespace</h3>
        <div class="help-text">
          When Orbital imports the OpenAPI spec, it will generate services within this namespace
        </div>
      </div>
      <div class="form-element">
        <tui-input [(ngModel)]="openApiPackageSpec.defaultNamespace" name="defaultNamespace">
          Default namespace
        </tui-input>
      </div>
    </div>
  `,
  styleUrls: ['./open-api-package-config.component.scss']
})
export class OpenApiPackageConfigComponent {

  @Input()
  openApiPackageSpec: OpenApiPackageLoaderSpec;

  @Input()
  path: string;

  @Output()
  pathChange = new EventEmitter<string>();

  @ViewChild('openApiPackageOrg')
  openApiPackageOrg$: NgModel

  @ViewChild('openApiPackageName')
  openApiPackageName$: NgControl
  @ViewChild('openApiPackageVersion')
  openApiPackageVersion$: NgControl


  onPathChanged(value: string) {
    this.path = value;
    this.pathChange.emit(value);
  }

  updateDefaultNamespace() {
    const org = this.openApiPackageSpec.identifier?.organisation || null;
    const name = this.openApiPackageSpec.identifier?.name || null;
    this.openApiPackageSpec.defaultNamespace = [org, name].filter(d => d !== null).join('.');
  }
}

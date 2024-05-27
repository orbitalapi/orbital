import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {CovalentCommonModule} from "@covalent/core/common";
import {NgIf} from "@angular/common";
import {TuiGroupModule, TuiNotificationModule} from "@taiga-ui/core";
import {TuiInputModule} from "@taiga-ui/kit";
import {PackageIdentifier} from "../package-viewer/packages.service";
import {NgControl, NgModel} from "@angular/forms";
import {ProjectSourceConfigModule} from "../project-import/project-source-config/project-source-config.module";
import {ValidIdentifierDirective} from "../project-import/project-source-config/valid-identifier.directive";
import {SemverValidatorDirective} from "../project-import/project-source-config/semver-validator.directive";

@Component({
  selector: 'app-package-identifier-input',
  standalone: true,
  imports: [
    CovalentCommonModule,
    NgIf,
    TuiGroupModule,
    TuiInputModule,
    TuiNotificationModule,
    ValidIdentifierDirective,
    SemverValidatorDirective,
  ],
  template: `
    <div tuiGroup>
      <tui-input [(ngModel)]="packageIdentifier.organisation" required name="openApiPackageOrg"
                 [readOnly]="!editable"
                 (ngModelChange)="updateDefaultNamespace()" validIdentifier #openApiPackageOrg="ngModel">
        Organisation
      </tui-input>
      <tui-input [(ngModel)]="packageIdentifier.name" required name="openApiPackageName"
                 [readOnly]="!editable"
                 (ngModelChange)="updateDefaultNamespace()" validIdentifier #openApiPackageName="ngModel">
        Name
      </tui-input>
      <tui-input [(ngModel)]="packageIdentifier.version" required name="openApiPackageVersion" semver
                 [readOnly]="!editable"
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
  `,
  styleUrl: './package-identifier-input.component.scss'
})
export class PackageIdentifierInputComponent {

  @Input()
  editable: boolean = true;

  @Input()
  packageIdentifier: PackageIdentifier;

  @Output()
  packageIdentifierChange = new EventEmitter<PackageIdentifier>()

  updateDefaultNamespace() {
    const org = this.packageIdentifier?.organisation || null;
    const name = this.packageIdentifier?.name || null;
    const namespace = [org, name].filter(d => d !== null).join('.');
    this.defaultNamespaceChange.emit(namespace)
  }

  @Output()
  defaultNamespaceChange = new EventEmitter<string>();

  @ViewChild('openApiPackageOrg')
  openApiPackageOrg$: NgModel

  @ViewChild('openApiPackageName')
  openApiPackageName$: NgControl
  @ViewChild('openApiPackageVersion')
  openApiPackageVersion$: NgControl
}

import { TuiInputModule } from "@taiga-ui/legacy";
import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {CovalentCommonModule} from "@covalent/core/common";
import {NgIf} from "@angular/common";
import { TuiError, TuiGroup } from '@taiga-ui/core';
import {PackageIdentifier} from "../package-viewer/packages.service";
import {ControlContainer, NgControl, NgModel, NgModelGroup, ReactiveFormsModule} from '@angular/forms';
import {ValidIdentifierDirective} from "../project-import/project-source-config/valid-identifier.directive";
import {SemverValidatorDirective} from "../project-import/project-source-config/semver-validator.directive";

@Component({
  selector: 'app-package-identifier-input',
  viewProviders: [{provide: ControlContainer, useExisting: NgModelGroup}],
  standalone: true,
  imports: [
    CovalentCommonModule,
    NgIf,
    TuiGroup,
    TuiInputModule,
    ValidIdentifierDirective,
    SemverValidatorDirective,
    TuiError,
    ReactiveFormsModule,
  ],
  template: `
    <div tuiGroup [collapsed]="true">
      <tui-input [(ngModel)]="packageIdentifier.organisation" required name="openApiPackageOrg"
                 [readOnly]="!editable"
                 (ngModelChange)="updateDefaultNamespace()" validIdentifier #openApiPackageOrg="ngModel">
        Organisation
        <span class="tui-required"></span>
      </tui-input>

      <tui-input [(ngModel)]="packageIdentifier.name" required name="openApiPackageName"
                 [readOnly]="!editable"
                 (ngModelChange)="updateDefaultNamespace()" validIdentifier #openApiPackageName="ngModel">
        Name
        <span class="tui-required"></span>
      </tui-input>
      <tui-input [(ngModel)]="packageIdentifier.version" required name="openApiPackageVersion" semver
                 [readOnly]="!editable"
                 #openApiPackageVersion="ngModel">
        Version
        <span class="tui-required"></span>
      </tui-input>
    </div>
    <tui-error [error]="openApiPackageOrg$ && openApiPackageOrg$.invalid && (openApiPackageOrg$.dirty || openApiPackageOrg$.touched)
      ? 'Organisation names must start with a letter, and only contain letters, underscores, hyphens, dots or numbers' : null"></tui-error>
    <tui-error [error]="openApiPackageOrg$ && openApiPackageName$.invalid && (openApiPackageName$.dirty || openApiPackageName$.touched)
      ? 'Package names must start with a letter, and only contain letters, underscores, hyphens or numbers' : null"></tui-error>
    <tui-error [error]="openApiPackageOrg$ && openApiPackageVersion$.invalid && (openApiPackageVersion$.dirty || openApiPackageVersion$.touched)
      ? 'Versions need to follow the convention of 0.0.0 (eg., 1.0.3)' : null"></tui-error>
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

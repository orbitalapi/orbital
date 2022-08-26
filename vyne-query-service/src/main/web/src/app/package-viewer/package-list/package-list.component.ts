import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { SourcePackageDescription } from '../packages.service';

@Component({
  selector: 'app-package-list',
  styleUrls: ['./package-list.component.scss'],
  template: `
    <div class="list-container">
      <div *ngFor="let sourcePackage of packages" class="source-package-card" (click)="packageClicked.emit(sourcePackage)">
        <h3 class="package-title">{{ sourcePackage.identifier.name }}</h3>
        <div class="tag-table">
          <table>
            <tr>
              <td class="tag-title">Version</td>
              <td>{{sourcePackage.identifier.version}}</td>
            </tr>
            <tr>
              <td class="tag-title">Organisation</td>
              <td>{{sourcePackage.identifier.organisation}}</td>
            </tr>
            <tr>
              <td class="tag-title">Status</td>
              <td><span [ngClass]="sourcePackage.health.status" class="status"> {{sourcePackage.health.status}}</span>
              </td>
            </tr>
          </table>
        </div>
      </div>
    </div>
  `
})
export class PackageListComponent {

  @Input()
  packages: SourcePackageDescription[];

  @Output()
  packageClicked = new EventEmitter<SourcePackageDescription>()
}

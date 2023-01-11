import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { SourcePackageDescription } from '../packages.service';

@Component({
  selector: 'app-package-list',
  styleUrls: ['./package-list.component.scss'],
  template: `
    <div class="list-container">
      <div *ngFor="let sourcePackage of packages" class="source-package-card"
           (click)="packageClicked.emit(sourcePackage)">
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
          <div class="icon-bar">
            <img [src]="getSourceIcon(sourcePackage)">
            <span class="small">{{getSourceDescription(sourcePackage)}}</span>
            <span class="spacer"></span>
            <img src="assets/img/tabler/lock-open.svg"  *ngIf="sourcePackage.editable">
            <span class="small" *ngIf="sourcePackage.editable">Editable</span>
          </div>
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})

export class PackageListComponent {

  @Input()
  packages: SourcePackageDescription[];

  @Output()
  packageClicked = new EventEmitter<SourcePackageDescription>()

  getSourceDescription(sourcePackage:SourcePackageDescription):string {
    switch (sourcePackage.publisherType) {
      case 'FileSystem': return 'Read from disk'
      case 'GitRepo': return 'Git repo'
      case 'Pushed': return 'Pushed to Vyne'
    }
  }
  getSourceIcon(sourcePackage: SourcePackageDescription) {
    switch (sourcePackage.publisherType) {
      case 'FileSystem': return 'assets/img/tabler/files.svg'
      case 'GitRepo': return 'assets/img/tabler/git-merge.svg'
      case 'Pushed': return 'assets/img/tabler/rss.svg'
    }
  }
}

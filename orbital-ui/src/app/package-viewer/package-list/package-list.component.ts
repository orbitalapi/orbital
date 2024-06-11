import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import { UiCustomisations } from '../../../environments/ui-customisations';
import {ProjectLoaderWithStatus, PublisherHealthStatus, SourcePackageDescription} from '../packages.service';
import {TuiStatus} from "@taiga-ui/kit";

@Component({
  selector: 'app-package-list',
  styleUrls: ['./package-list.component.scss'],
  template: `
    <div class="list-container">
      <div *ngIf="projectsWithProblems?.length > 0" class="source-package-card error-state"
           (click)="showProjectsWithProblems.emit()">
        <img src="assets/img/tabler/exclamation-circle.svg">
        <h3 class="package-title">{{ projectsWithProblems.length }} of your projects has a configuration problem</h3>
      </div>
      <div
        *ngFor="let sourcePackage of packages"
        [routerLink]="sourcePackage.uriPath"
        routerLinkActive="selected-list-item"
        class="source-package-card"
      >
        <h3 class="package-title">
          {{ sourcePackage.identifier.name }}
          <tui-badge *ngIf="getPackageBadgeState(sourcePackage) === 'error'"
                     [status]="getPackageBadgeState(sourcePackage)" size="s"
                     [value]="getPackageStateBadgeMessage(sourcePackage)"></tui-badge>
        </h3>
        <div *ngIf="getPackageStateMessage(sourcePackage)" class="unhealthy-state">
          {{ getPackageStateMessage(sourcePackage) }}
        </div>
        <div class="tag-table">
          <table>
            <tr>
              <td class="tag-title">Version</td>
              <td>{{ sourcePackage.identifier.version }}</td>
            </tr>
            <tr>
              <td class="tag-title">Organisation</td>
              <td>{{ sourcePackage.identifier.organisation }}</td>
            </tr>
          </table>
          <div class="icon-bar">
            <img [src]="getSourceIcon(sourcePackage)">
            <span class="small">{{ getSourceDescription(sourcePackage) }}</span>
            <span class="spacer"></span>
            <img src="assets/img/tabler/lock-open.svg" *ngIf="sourcePackage.editable">
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
  projectsWithProblems: ProjectLoaderWithStatus[]

  @Input()
  packages: SourcePackageDescription[];

  @Input()
  packagesWithCompilationErrors: string[] = [];

  @Output()
  showProjectsWithProblems = new EventEmitter()

  @Output()
  packageClicked = new EventEmitter<SourcePackageDescription>()

  getSourceDescription(sourcePackage: SourcePackageDescription): string {
    switch (sourcePackage.publisherType) {
      case 'FileSystem':
        return 'Read from disk'
      case 'GitRepo':
        return 'Git repo';
      case 'Pushed':
        return `Pushed to ${UiCustomisations.productName}`;
    }
  }

  getSourceIcon(sourcePackage: SourcePackageDescription) {
    switch (sourcePackage.publisherType) {
      case 'FileSystem':
        return 'assets/img/tabler/file.svg'
      case 'GitRepo':
        return 'assets/img/tabler/git-merge.svg'
      case 'Pushed':
        return 'assets/img/tabler/rss.svg'
    }
  }

  getPackageStateBadgeMessage(sourcePackage: SourcePackageDescription): PublisherHealthStatus {
    if (this.packagesWithCompilationErrors.includes(sourcePackage.identifier.id)) {
      return "Unhealthy"
    } else {
      return sourcePackage.health.status
    }
  }

  getPackageStateMessage(sourcePackage: SourcePackageDescription) {
    if (this.getPackageStateBadgeMessage(sourcePackage) !== "Unhealthy") {
      return null
    }
    if (this.packagesWithCompilationErrors.includes(sourcePackage.identifier.id)) {
      return "Contains compilation errors"
    } else return sourcePackage.health.message
  }

  getPackageBadgeState(sourcePackage: SourcePackageDescription): TuiStatus {
    if (this.packagesWithCompilationErrors.includes(sourcePackage.identifier.id)) {
      return "error";
    } else {
      switch (sourcePackage.health.status) {
        case "Healthy":
          return "success"
        case "Unhealthy":
          return "error"
        case "Unknown":
          return "neutral"
      }
    }
  }
}

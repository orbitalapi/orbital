import {
  ChangeDetectionStrategy,
  Component, computed,
  EventEmitter,
  input,
  Input,
  InputSignal,
  Output, signal,
  Signal, WritableSignal
} from '@angular/core';
import {TuiAppearanceOptions} from '@taiga-ui/core';
import { UiCustomisations } from '../../../environments/ui-customisations';
import {ProjectLoaderWithStatus, PublisherHealthStatus, SourcePackageDescription} from '../packages.service';
import {TuiStatus} from "@taiga-ui/kit";

type SortOrder = 'A-Z' | 'Updated'

@Component({
  selector: 'app-package-list',
  styleUrls: ['./package-list.component.scss'],
  template: `
    <div class="list-container">
      <div class="header">
        <app-dropdown [value]="sortOrder()" hint="Sort order" [(isMenuOpen)]="sortOrderDropdownOpen">
          <tui-data-list>
            @for (so of sortOrders; track so) {
              <button tuiOption (click)="sortOrder.set(so); sortOrderDropdownOpen = false">
                {{ so }}
                <tui-icon  *ngIf="so === sortOrder()" icon="@tui.check"></tui-icon>
              </button>
            }
          </tui-data-list>
        </app-dropdown>
      </div>
      <div *ngIf="projectsWithProblems?.length > 0" class="source-package-card error-state"
           (click)="showProjectsWithProblems.emit()">
        <img src="assets/img/tabler/exclamation-circle.svg">
        <h3 class="package-title">{{ projectsWithProblems.length }} of your projects has a configuration problem</h3>
      </div>
      <div
        *ngFor="let sourcePackage of sortedPackages()"
        [routerLink]="sourcePackage.uriPath"
        routerLinkActive="selected-list-item"
        class="source-package-card"
      >
        <h3 class="package-title">
          {{ sourcePackage.identifier.name }}
          <tui-badge *ngIf="getPackageBadgeState(sourcePackage) === 'error'"
                     [appearance]="getPackageBadgeState(sourcePackage)" size="m"
                    >{{ getPackageStateBadgeMessage(sourcePackage) }}</tui-badge>
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

  packages: InputSignal<SourcePackageDescription[]> = input<SourcePackageDescription[]>();
  sortedPackages: Signal<SourcePackageDescription[]> = computed(() => {
    return this.packages()?.sort((a, b) => {
      if (this.sortOrder() === 'A-Z') {
        return a.identifier.name.localeCompare(b.identifier.name)
      } else if (this.sortOrder() === 'Updated') {
        return new Date(b.submissionDate).valueOf() - new Date(a.submissionDate).valueOf();
      }
    })
  })

  @Input()
  packagesWithCompilationErrors: string[] = [];

  @Output()
  showProjectsWithProblems = new EventEmitter()

  @Output()
  packageClicked = new EventEmitter<SourcePackageDescription>()

  sortOrders: SortOrder[] = ['A-Z', 'Updated']
  sortOrder: WritableSignal<SortOrder> = signal('A-Z');
  sortOrderDropdownOpen: boolean

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

  getPackageBadgeState(sourcePackage: SourcePackageDescription): TuiAppearanceOptions["appearance"] {
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

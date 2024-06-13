import {ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef} from '@angular/core';
import {TuiHintModule} from '@taiga-ui/core';
import {ChangelogModule} from './changelog.module';
import {ChangeLogEntry, ChangelogService} from "./changelog.service";
import {SchemaNotificationService} from "../services/schema-notification.service";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {Observable, switchMap} from "rxjs";
import {startWith, tap} from "rxjs/operators";
import {AsyncPipe, CommonModule, I18nPluralPipe} from '@angular/common';
import {MomentModule} from "ngx-moment";
import {Router, RouterLink} from '@angular/router';
import {PackagesService, SourcePackageDescription} from "../package-viewer/packages.service";

@Component({
  selector: 'app-changelog-timeline',
  standalone: true,
  imports: [
    CommonModule,
    AsyncPipe,
    MomentModule,
    RouterLink,
    I18nPluralPipe,
    TuiHintModule,
    ChangelogModule
  ],
  template: `
    <ul class="timeline">
      <li class="timeline-item" *ngFor="let changelogEntry of changelogEntries$ | async; let last = last">
        <div class="timeline-left">{{ changelogEntry.timestamp | amCalendar }}</div>
        <div class="timeline-center">
          <div class="timeline-entry-icon"></div>
          <div *ngIf="!last" class="vertical-line"></div>
        </div>
        <div class="timeline-right">
          <div class="changelog-package" *ngFor="let pkg of changelogEntry.affectedPackages">
            <img src="assets/img/tabler/package.svg">
            <div>
              {{ pkg.split('/')[0] }} / <a class="link" [routerLink]="packageIdToRoute(pkg)">{{ pkg.split('/')[1] }}</a>
              <div class="diff-length" [tuiHint]="hintTemplate" tuiHintAppearance="onDark">
                {{ changelogEntry.diffs.length | i18nPlural: changelogPluralMap }}
              </div>
              <ng-template #hintTemplate><app-diff-list [diffs]="changelogEntry.diffs"></app-diff-list></ng-template>
            </div>
          </div>
        </div>
      </li>
    </ul>
  `,
  styleUrl: './changelog-timeline.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChangelogTimelineComponent {

  changelogEntries$: Observable<ChangeLogEntry[]>
  private packages: SourcePackageDescription[];

  changelogPluralMap = {
    '=0': 'no changes',
    '=1': '1 change',
    'other': '# changes'
  }

  constructor(
    private changeDetector: ChangeDetectorRef,
    private changelogService: ChangelogService,
    private packageService: PackagesService,
    private schemaNotificationService: SchemaNotificationService,
    private destroyRef: DestroyRef,
    private router: Router
  ) {

    this.changelogEntries$ = this.schemaNotificationService.createSchemaNotificationsSubscription()
      .pipe(
        startWith(null),
        tap(next => {
          packageService.listPackages()
            .pipe(takeUntilDestroyed(destroyRef))
            .subscribe(next => this.packages = next)
          this.changeDetector.markForCheck();
        }),
        switchMap(next => this.changelogService.getChangelog())
      )
  }

  packageIdToRoute(packageId: string): string[] {
    if (this.packages?.length > 0) {
      const packageDescription = this.packages.find(pkg => pkg.identifier.id.startsWith(packageId))
      if (!packageDescription) {
        return [];
      }
      const uriPath = packageDescription.uriPath
      return this.router.url.includes('projects') ? [uriPath, 'changelog'] : ['projects', uriPath, 'changelog'];
    } else {
      return [];
    }
  }

}

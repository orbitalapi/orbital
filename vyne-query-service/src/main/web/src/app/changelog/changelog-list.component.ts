import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { ChangeLogDiffEntry, ChangeLogEntry } from './changelog.service';

@Component({
  selector: 'app-changelog-list',
  template: `
    <div class="changelog-entry" *ngFor="let changeLogEntry of changeLogEntries">
      <div class="changelog-header">
        <div class="h3">
          {{ changeLogEntry.timestamp | date:'longDate' }}
        </div>
        <div class="h4">{{ changeLogEntry.timestamp | amTimeAgo }}</div>
      </div>
      <div class="h3 affected-package">{{ changeLogEntry.affectedPackages[0] }}</div>

      <div class="diff-list-container">
        <app-diff-entry *ngFor="let diff of changeLogEntry.diffs" [diff]="diff">

        </app-diff-entry>
      </div>

    </div>

  `,
  styleUrls: ['./changelog-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChangelogListComponent {

  @Input()
  changeLogEntries: ChangeLogEntry[]
}

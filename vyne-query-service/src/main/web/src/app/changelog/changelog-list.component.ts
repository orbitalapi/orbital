import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { ChangeLogDiffEntry, ChangeLogEntry } from './changelog.service';

@Component({
  selector: 'app-changelog-list',
  template: `
    <div class="changelog-entry" *ngFor="let changeLogEntry of changeLogEntries">
      <div class="changelog-header">
        <h2>
          {{ changeLogEntry.timestamp | date:'longDate' }}
        </h2>
        <span>{{ changeLogEntry.timestamp | amTimeAgo }}</span>
        <h3>{{ changeLogEntry.affectedPackages[0] }}</h3>
      </div>
      <div class="diff-list-container">
        <h3>Changes</h3>
        <app-diff-entry *ngFor="let diff of changeLogEntry.diffs" [diff]="diff">

        </app-diff-entry>
      </div>
    </div>
    <div class="empty-state" *ngIf="changeLogEntries.length === 0">
      <p>Looks like we don't have any changelogs for this package yet.</p>
    </div>

  `,
  styleUrls: ['./changelog-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChangelogListComponent {

  @Input()
  changeLogEntries: ChangeLogEntry[]
}

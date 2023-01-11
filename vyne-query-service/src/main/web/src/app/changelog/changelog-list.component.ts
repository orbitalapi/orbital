import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { ChangeLogDiffEntry, ChangeLogEntry } from './changelog.service';
import { paginate } from 'src/app/utils/arrays';

@Component({
  selector: 'app-changelog-list',
  template: `
    <div class="changelog-entry" *ngFor="let changeLogEntry of currentChangeLogEntryPage">
      <div class="changelog-header">
        <h2>
          {{ changeLogEntry.timestamp | date:'longDate' }}
        </h2>
        <span>{{ changeLogEntry.timestamp | amTimeAgo }}</span>
        <h3>{{ changeLogEntry.affectedPackages[0] }}</h3>
      </div>
      <div class="diff-list-container">
        <h3>{{changeLogEntry.diffs.length}} Changes</h3>
        <app-diff-list [diffs]="changeLogEntry.diffs"></app-diff-list>
      </div>
    </div>
    <div class="empty-state" *ngIf="!changeLogEntries || changeLogEntries.length === 0">
      <p>Looks like we don't have any changelogs for this package yet.</p>
    </div>
    <tui-pagination
      *ngIf="changeLogEntryPages.length > 1"
      [length]="changeLogEntryPages.length"
      [index]="changeLogEntryPageIndex"
      (indexChange)="setChangeLogEntryPage($event)"
    ></tui-pagination>

  `,
  styleUrls: ['./changelog-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChangelogListComponent {
  changeLogEntryPageIndex: number = 0;
  private _changeLogEntries: ChangeLogEntry[]
  changeLogEntryPages: ChangeLogEntry[][] = [];

  currentChangeLogEntryPage: ChangeLogEntry[] = [];

  @Input()
  get changeLogEntries(): ChangeLogEntry[] {
    return this._changeLogEntries;
  }

  set changeLogEntries(value: ChangeLogEntry[]) {
    if (this._changeLogEntries === value) {
      return;
    }
    this._changeLogEntries = value;
    this.paginateChangelogEntries()
  }

  setChangeLogEntryPage(index: number) {
    this.changeLogEntryPageIndex = index;
    if (index === 0 && this.changeLogEntryPages.length === 0) {
      this.currentChangeLogEntryPage = [];
    } else {
      this.currentChangeLogEntryPage = this.changeLogEntryPages[index];
    }
  }

  private paginateChangelogEntries() {
    this.changeLogEntryPages = paginate(this.changeLogEntries, 5)
    this.setChangeLogEntryPage(0);
  }
}

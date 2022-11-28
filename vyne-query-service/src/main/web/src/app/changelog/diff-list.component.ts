import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { ChangeLogDiffEntry } from 'src/app/changelog/changelog.service';
import { paginate } from 'src/app/utils/arrays';

@Component({
  selector: 'app-diff-list',
  template: `
    <app-diff-entry *ngFor="let diff of currentPage" [diff]="diff">
    </app-diff-entry>
    <tui-pagination
      *ngIf="diffPages.length > 1"
      [length]="diffPages.length"
      [(index)]="pageIndex"
    ></tui-pagination>
  `,
  styleUrls: ['./diff-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DiffListComponent {

  private _diffs: ChangeLogDiffEntry[]
  pageIndex: number = 0;
  diffPages: ChangeLogDiffEntry[][] = [];

  get currentPage():ChangeLogDiffEntry[] {
    if (this.diffPages.length === 0) {
      return []
    } else {
      return this.diffPages[this.pageIndex];
    }
  }

  @Input()
  get diffs(): ChangeLogDiffEntry[] {
    return this._diffs;
  }

  set diffs(value: ChangeLogDiffEntry[]) {
    if (value === this._diffs) {
      return;
    }
    this._diffs = value;
    this.buildPaginatedList()
  }


  private buildPaginatedList() {
    this.diffPages = paginate(this.diffs, 10)
    this.pageIndex = 0;
  }
}

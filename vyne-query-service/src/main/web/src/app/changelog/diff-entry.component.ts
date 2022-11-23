import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { ChangeLogDiffEntry, DiffKind } from './changelog.service';

@Component({
  selector: 'app-diff-entry',
  template: `
    <div class="row">
      <tui-svg [src]="iconForDiffKind" class="icon" [ngClass]="diffCategory"></tui-svg>
      <span class="display-name badge" [ngClass]="diffCategory">{{ diff.displayName }}</span>
      <span class="diff-kind subtle">{{ diff.kind | diffKind}}</span>
    </div>
    <div class="row">
      <app-type-change [label]="'Return type changed: '" class="details-block"
                       *ngIf="diff.kind === 'OperationReturnValueChanged'"
                       [oldValue]="diff.oldDetails"
                       [newValue]="diff.newDetails"></app-type-change>
      <app-input-params-change class="details-block" *ngIf="diff.kind === 'OperationParametersChanged'"
                               [oldValue]="diff.oldDetails"
                               [newValue]="diff.newDetails"
      >

      </app-input-params-change>
      <app-metadata-change class="details-block" *ngIf="diff.kind === 'OperationMetadataChanged'"
                           [oldValue]="diff.oldDetails!"
                           [newValue]="diff.newDetails!"
      ></app-metadata-change>
      <app-metadata-change class="details-block" *ngIf="diff.kind === 'MetadataChanged'"
                           [oldValue]="diff.oldDetails!"
                           [newValue]="diff.newDetails!"
      >
      </app-metadata-change>
      <app-documentation-change class="details-block" *ngIf="diff.kind === 'DocumentationChanged'"
                                [oldValue]="diff.oldDetails!"
                                [newValue]="diff.newDetails!"></app-documentation-change>

    </div>
    <div>

      <div class="children-container">
        <app-diff-entry *ngFor="let childDiff of diff.children" [diff]="childDiff"></app-diff-entry>
      </div>
    </div>

  `,
  styleUrls: ['./diff-entry.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DiffEntryComponent {

  @Input()
  diff: ChangeLogDiffEntry

  get diffCategory(): 'add' | 'remove' | 'change' | 'unknown' {
    if (!this.diff) {
      return 'unknown';
    }
    if (this.diff.kind.includes('Added')) {
      return 'add';
    }
    if (this.diff.kind.includes('Removed')) {
      return 'remove';
    }
    if (this.diff.kind.includes('Changed')) {
      return 'change'
    }
    return 'unknown';
  }

  get iconForDiffKind(): string {
    switch (this.diffCategory) {
      case 'add':
        return 'tuiIconPlus';
      case 'change':
        return 'tuiIconDraft';
      case 'remove':
        return 'tuiIconMinus';
      default:
        return '';
    }
  }

}

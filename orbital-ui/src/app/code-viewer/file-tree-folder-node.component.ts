import { Component } from '@angular/core';
import {TuiTreeItemContentComponent} from "@taiga-ui/kit";

/**
 * This whole class is really only needed because the base component
 * doesn't rotate the "chevron" icon when clicked.
 * See https://github.com/taiga-family/taiga-ui/issues/6834
 */
@Component({
  selector: 'app-file-tree-folder-node',
  template: `
    <tui-svg
      (click)="onClick()"
      [ngClass]="iconClass"
      [src]="icon"
    ></tui-svg>
    <ng-container [ngTemplateOutlet]="context.template"></ng-container>
  `,
  styleUrls: ['./file-tree-folder-node.component.scss'],
})
export class FileTreeFolderNodeComponent extends TuiTreeItemContentComponent {
  get icon(): string {
    return this.isExpandable ? 'assets/img/tabler/chevron-right.svg' : 'assets/img/tabler/align-left.svg';
  }
  get iconClass() {
    return {
      'expanded' : this.isExpandable && this.isExpanded,
      'expandable' : this.isExpandable
    }
  }
}{

}

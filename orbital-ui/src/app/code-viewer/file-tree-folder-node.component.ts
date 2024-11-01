import { Component } from '@angular/core';
import {TuiTreeItemContent} from "@taiga-ui/kit";

/**
 * This whole class is really only needed because the base component
 * doesn't rotate the "chevron" icon when clicked.
 * See https://github.com/taiga-family/taiga-ui/issues/6834
 */
@Component({
  selector: 'app-file-tree-folder-node',
  template: `
    <tui-icon
      (click)="onClick()"
      [ngClass]="iconClass"
      [icon]="icon"
    ></tui-icon>
    <ng-container [ngTemplateOutlet]="context.template"></ng-container>
  `,
  styleUrls: ['./file-tree-folder-node.component.scss'],
})
export class FileTreeFolderNodeComponent extends TuiTreeItemContent {
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

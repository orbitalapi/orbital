import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output} from '@angular/core';
import {VersionedSource} from "../services/schema";
import {EMPTY_ARRAY, TuiHandler} from "@taiga-ui/cdk";
import {TUI_TREE_CONTENT, TuiTreeItemComponent} from "@taiga-ui/kit";
import {PolymorpheusComponent} from "@tinkoff/ng-polymorpheus";
import {FileTreeFolderNodeComponent} from "./file-tree-folder-node.component";
import {ActivatedRoute, Router} from "@angular/router";
import {integer} from "vscode-languageclient";

@Component({
  selector: 'app-file-tree',
  template: `
    <tui-tree [childrenHandler]="handler"
              [content]="content"
              [tuiTreeController]="false"
              [map]="nodeStatus"
              [value]="tree"></tui-tree>
    <ng-template
      #content
      let-node="node"
      let-value
    >
      <div class="row" (click)="onClick(value)" [ngClass]="{'active': value.value?.filename === selectedFilename}">
        <button class="tree-node" [ngClass]="{'has-error': value.hasErrors}">{{ value.label }}</button>
        <tui-badge status="error" *ngIf="value.value?.errorCount > 0" size="xs">{{ value.value.errorCount }}</tui-badge>
      </div>
    </ng-template>
  `,
  styleUrls: ['./file-tree.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: TUI_TREE_CONTENT,
      useValue: new PolymorpheusComponent(FileTreeFolderNodeComponent),
    },
  ],
})
export class FileTreeComponent {

  hasError(node: FilenameWithDecorators): boolean {
    return node.errorCount > 0;
  }

  constructor(private changeRef: ChangeDetectorRef) {
  }

  @Input()
  selectedFilename: string;

  @Output()
  itemClicked = new EventEmitter<string>()

  readonly nodeStatus = new Map<TreeNode, boolean>();

  readonly handler: TuiHandler<TreeNode, readonly TreeNode[]> = item =>
    item.childNodes || EMPTY_ARRAY;

  tree: TreeNode;

  private filenameWithDecorators: FilenameWithDecorators[];
  private _filenames: string[] | FilenameWithDecorators[];
  @Input()
  get filenames(): string[] | FilenameWithDecorators[] {
    return this._filenames;
  }

  set filenames(value) {
    this._filenames = value;
    this.filenameWithDecorators = this._filenames.map(filename => {
      if (typeof filename === 'string') {
        return {
          errorCount: 0,
          filename: filename
        } as FilenameWithDecorators
      } else {
        return filename
      }
    })
    this.tree = this.createTree();
    this.nodeStatus.clear();
    [this.tree, ...this.tree.descendants].forEach(node => this.nodeStatus.set(node, true))
  }

  toggleExpandedStatus(treeNode: TreeNode) {
    this.nodeStatus.set(treeNode, !this.nodeStatus.get(treeNode))
  }

  onClick(treeNode: TreeNode) {
    if (treeNode.value) {
      this.itemClicked.emit(treeNode.value.filename)
    } else {
      this.toggleExpandedStatus(treeNode);
    }
  }

  private createTree() {
    const root: TreeNode = new TreeNode('/')
    this.filenameWithDecorators.forEach(filePath => {
      const parts = filePath.filename.split("/")
      const directoryParts = parts.slice(0, -1) // drop the last element
      const leaf = directoryParts.reduce((acc:TreeNode, currentValue:string) => acc.getOrCreateChild(currentValue), root)
      const filename = parts[parts.length - 1];
      leaf.addChild(new TreeNode(filename, filePath))
    })
    return root;
  }
}

class TreeNode {
  constructor(public readonly label: string, public readonly value: FilenameWithDecorators | null = null) {
  }

  children: { [key: string]: TreeNode } = {};

  get childNodes(): TreeNode[] {
    return Object.values(this.children);
  }

  get hasErrors(): boolean {
    if (this.value?.errorCount > 0) {
      return true;
    }
    return this.descendants.some(d => d.hasErrors);
  }


  get descendants(): TreeNode[] {
    const childDescendants = this.childNodes.map(it => it.descendants).flat()
    return this.childNodes.concat(...childDescendants)
  }

  addChild(node: TreeNode): TreeNode {
    this.children[node.label] = node;
    return node
  }

  getOrCreateChild(key: string) {
    if (!this.children[key]) {
      return this.newChild(key)
    } else {
      return this.children[key]
    }
  }

  newChild(name: string): TreeNode {
    return this.addChild(new TreeNode(name));
  }

}

export interface FilenameWithDecorators {
  filename: string;
  errorCount: number;
}

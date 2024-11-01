import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output} from '@angular/core';
import {ParsedSource, VersionedSource} from "../services/schema";
import {EMPTY_ARRAY, TuiHandler} from "@taiga-ui/cdk";
import {TUI_TREE_CONTENT} from "@taiga-ui/kit";
import {PolymorpheusComponent} from "@taiga-ui/polymorpheus";
import {FileTreeFolderNodeComponent} from "./file-tree-folder-node.component";
import {convertToParsedSources} from "./code-viewer.component";

@Component({
  selector: 'app-file-tree',
  template: `
    <tui-tree [childrenHandler]="handler"
              [content]="content"
              [tuiTreeController]="false"
              [map]="nodeStatus"
              *ngFor="let treeNode of tree"
              [value]="treeNode"></tui-tree>
    <ng-template
      #content
      let-node="node"
      let-value
    >
      <div class="row" (click)="onClick(value)" [ngClass]="{'active': value.value?.filename === selectedFilename}">
        <button class="tree-node" [ngClass]="{'has-error': value.hasErrors, 'has-warning': value.hasWarnings && !value.hasErrors}">{{ value.label }}</button>
        <tui-badge appearance="error" *ngIf="value.value?.errorCount > 0" size="s">12</tui-badge>
        <tui-badge appearance="warning" *ngIf="value.value?.warningCount > 0" size="s">{{ value.value.warningCount }}</tui-badge>
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

  hasError(node: FileEntryWithErrorCounts): boolean {
    return node.errorCount > 0;
  }

  constructor(private changeRef: ChangeDetectorRef) {
  }

  @Input()
  selectedFilename: string;

  @Output()
  itemClicked = new EventEmitter<string>()

  readonly nodeStatus = new Map<FileTreeNode, boolean>();

  readonly handler: TuiHandler<FileTreeNode, readonly FileTreeNode[]> = item =>
    item.childNodes || EMPTY_ARRAY;

  private _tree: FileTreeNode[];

  @Input()
  get tree(): FileTreeNode[] {
    return this._tree;
  }

  set tree(value: FileTreeNode[]) {
    this._tree = value;
    this.resetTreeState(value);
  }

  private filenameWithDecorators: FileEntryWithErrorCounts[];
  private _files: string[] | FileEntryWithErrorCounts[] | FileTreeNode[];
  @Input()
  get files(): string[] | FileEntryWithErrorCounts[] | FileTreeNode[] {
    return this._files;
  }

  set files(value) {
    if (value === this._files) {
      return;
    }
    this._files = value;

    if (isFileTreeNodeList(value)) {
      this.tree = value;
    } else {
      this.filenameWithDecorators = this._files.map(filename => {
        if (typeof filename === 'string') {
          return {
            errorCount: 0,
            filename: filename
          } as FileEntryWithErrorCounts
        } else {
          return filename
        }
      })
      const treeRoot = createTree("/", this.filenameWithDecorators);
      this.tree = [treeRoot]
    }

  }


  private resetTreeState(treeRoots: FileTreeNode[]) {
    this.nodeStatus.clear();
    treeRoots.forEach(treeRoot => {
      [treeRoot, ...treeRoot.descendants].forEach(node => this.nodeStatus.set(node, true))
    })
  }

  toggleExpandedStatus(treeNode: FileTreeNode) {
    this.nodeStatus.set(treeNode, !this.nodeStatus.get(treeNode))
  }

  onClick(treeNode: FileTreeNode) {
    if (treeNode.value) {
      this.itemClicked.emit(treeNode.value.filename)
    } else {
      this.toggleExpandedStatus(treeNode);
    }
  }
}

export function sourcesToFileTreeNode(sources: ParsedSource[] | VersionedSource[], rootNodeName: string): FileTreeNode {
  const parsedSources: ParsedSource[] = convertToParsedSources(sources);
  const filenamesWithDecorators: FileEntryWithErrorCounts[] = parsedSources.map(parsedSource => {
    return parsedSourceToDecoratedFileEntry(parsedSource)
  });
  return createTree(rootNodeName, filenamesWithDecorators)
}

export function parsedSourceToDecoratedFileEntry(parsedSource: ParsedSource): FileEntryWithErrorCounts {
  return {
    filename: parsedSource.source.name,
    errorCount: parsedSource.errors.filter(error => error.severity === 'ERROR').length,
    warningCount: parsedSource.errors.filter(error => error.severity === 'WARNING').length,
    source: parsedSource
  }
}

export function createTree(rootNodeName: string, files: FileEntryWithErrorCounts[]) {
  const root: FileTreeNode = new FileTreeNode(rootNodeName)
  files.forEach((file: FileEntryWithErrorCounts) => {
    const parts = file.filename.split("/")
    const directoryParts = parts.slice(0, -1) // drop the last element
    const leaf = directoryParts.reduce((acc: FileTreeNode, currentValue: string) => acc.getOrCreateChild(currentValue), root)
    const filename = parts[parts.length - 1];
    leaf.addChild(new FileTreeNode(filename, file))
  })
  return root;
}

export class FileTreeNode {
  constructor(public readonly label: string, public readonly value: FileEntryWithErrorCounts | null = null) {
  }

  children: { [key: string]: FileTreeNode } = {};

  get childNodes(): FileTreeNode[] {
    return Object.values(this.children);
  }

  get hasErrors(): boolean {
    if (this.value?.errorCount > 0) {
      return true;
    }
    return this.descendants.some(d => d.hasErrors);
  }

  get hasWarnings(): boolean {
    if (this.value?.warningCount > 0) {
      return true;
    }
    return this.descendants.some(d => d.hasWarnings);
  }

  get descendants(): FileTreeNode[] {
    const childDescendants = this.childNodes.map(it => it.descendants).flat()
    return this.childNodes.concat(...childDescendants)
  }

  addChild(node: FileTreeNode): FileTreeNode {
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

  newChild(name: string): FileTreeNode {
    return this.addChild(new FileTreeNode(name));
  }

}

export interface FileEntryWithErrorCounts {
  filename: string;
  errorCount: number;
  warningCount: number;
  source: ParsedSource
}

export function isFileTreeNodeList(value: any): value is FileTreeNode[] {
  if (Array.isArray(value)) {
    if (value.length === 0) {
      return false;
    } else {
      return value[0] instanceof FileTreeNode;
    }
  } else {
    return false;
  }
}

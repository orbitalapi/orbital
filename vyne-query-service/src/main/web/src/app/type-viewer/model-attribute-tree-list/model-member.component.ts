import { ChangeDetectionStrategy, Component, EventEmitter, Inject, Input, Output } from '@angular/core';
import { Field, findType, QualifiedName, Schema, Type } from '../../services/schema';
import { isNullOrUndefined } from 'util';
import { TuiHandler } from '@taiga-ui/cdk';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { TypeSearchContainerComponent } from '../type-search/type-search-container.component';
import { BaseDeferredEditComponent } from '../base-deferred-edit.component';
import { TUI_TREE_LOADING, TuiTreeLoader, TuiTreeService } from '@taiga-ui/kit';
import { Observable, of } from 'rxjs';
import { TypeSelectedEvent } from 'src/app/type-viewer/type-search/type-selected-event';


export interface TypeMemberTreeNode {
  name: string;
  type: Type;
  field: Field;
  isNew: boolean;
  children: TypeMemberTreeNode[];
  isRoot: boolean;
  isLastChild: boolean;
  editingDescription: boolean;
}

@Component({
  selector: 'app-model-member',
  template: `
    <div *ngIf="treeDataService">
      <tui-tree
        [tuiTreeController]="false"
        [value]="treeDataService.data$ | async"
        [content]="treeContent"
        [childrenHandler]="treeChildrenHandler"
        (toggled)="onToggled($event)"
        [map]="map"
      ></tui-tree>
      <ng-template #treeContent let-item>
        <div class="tree-node" [ngClass]="{child: !item.isRoot, isLastChild: item.isLastChild}">
          <app-model-member-tree-node [treeNode]="item" [editable]="editable"
                                      [showFullTypeNames]="showFullTypeNames"
                                      (nodeUpdated)="updateDeferred.emit(member)"
                                      (typeNameClicked)="onTypeNameClicked(item)"
          ></app-model-member-tree-node>
        </div>

      </ng-template>

    </div>


  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./model-member.component.scss']
})
export class ModelMemberComponent extends BaseDeferredEditComponent<Field> {

  constructor(private dialog: MatDialog) {
    super();
  }

  unloadedChildrenPlaceholder: TypeMemberTreeNode;

  map = new Map<TypeMemberTreeNode, boolean>();

  private _member: Field;

  @Input()
  showFullTypeNames = false;

  @Input()
  editable: boolean = false;

  descriptionEditable = false;

  @Output()
  typeNameClicked = new EventEmitter<QualifiedName>()

  @Input()
  new: boolean = false;
  treeData: TypeMemberTreeNode;

  treeDataService: TuiTreeService<TypeMemberTreeNode>;

  @Input()
  get member(): Field {
    return this._member;
  }

  set member(value: Field) {
    if (this.member === value) {
      return
    }
    this._member = value;
    this.setMemberType()
  }

  get type(): Field {
    return this.member;
  }

  private _schema: Schema

  @Input()
  get schema(): Schema {
    return this._schema;
  }

  set schema(value: Schema) {
    if (this.schema === value) {
      return;
    }
    this._schema = value;
    this.setMemberType();
  }

  private _anonymousTypes: Type[];

  @Input()
  get anonymousTypes(): Type[] {
    return this._anonymousTypes;
  }

  set anonymousTypes(value) {
    if (this._anonymousTypes === value) {
      return;
    }
    this._anonymousTypes = value;
    this.setMemberType();
  }


  @Input()
  memberName: string;

  memberType: Type

  private setMemberType() {
    if (isNullOrUndefined(this.schema) || isNullOrUndefined(this.member) || isNullOrUndefined(this.anonymousTypes)) {
      return;
    }
    this.memberType = findType(this.schema, this.member.type.parameterizedName, this.anonymousTypes);
    this.new = this.anonymousTypes.includes(this.memberType);
    const treeData = this.buildTreeRootNode();
    this.unloadedChildrenPlaceholder = {
      name: 'Loading...',
      children: [],
      editingDescription: false,
      isNew: false,
      isRoot: false,
      field: treeData.field,
      type: treeData.type,
      isLastChild: false
    }
    this.treeDataService = new TuiTreeService<TypeMemberTreeNode>(
      this.unloadedChildrenPlaceholder, treeData, new TreeLoader()
    )
    this.treeData = treeData;

  }

  makeDescriptionEditable(item: TypeMemberTreeNode, editable: boolean) {
    if (this.editable) {
      item.editingDescription = editable;
    }
  }

  private readonly loadedChildren = new Set<TypeMemberTreeNode>();

  onToggled(item: TypeMemberTreeNode): void {
    this.treeDataService.loadChildren(item);
  }

  treeChildrenHandler: TuiHandler<TypeMemberTreeNode, readonly TypeMemberTreeNode[]> = item => {
    if (item.children.length > 0) {
      return this.treeDataService.getChildren(item);
    } else {
      return item.children;
    }
  };

  private buildTreeRootNode(): TypeMemberTreeNode {
    return this.logDuration('buildTreeRootNode', () => {
      return {
        name: this.memberName,
        field: this.member,
        type: this.memberType,
        children: this.buildTreeData(this.memberType),
        isRoot: true,
        isLastChild: !this.memberType.isScalar,
        editingDescription: false,
        isNew: this.new
      }
    });
  }

  private logDuration<T>(name: string, callback: () => T): T {
    const startTime = new Date().getTime();
    const response = callback();
    const endTime = new Date().getTime();
    console.log(`${name} completed in ${endTime - startTime}ms`);
    return response;
  }

  private buildTreeData(memberType: Type): TypeMemberTreeNode[] {
    if (memberType.isCollection) {
      return this.buildTreeData(memberType.collectionType);
    }
    if (isNullOrUndefined(memberType.attributes)) {
      return [];
    }
    const nodes = Object.keys(memberType.attributes).map(key => {
      const field = memberType.attributes[key];
      const fieldType = findType(this.schema, memberType.attributes[key].type.parameterizedName, this.anonymousTypes);
      if (fieldType === null || fieldType === undefined) {
        throw new Error('No fieldType found');
      }
      return {
        name: key,
        field,
        type: fieldType,
        children: this.buildTreeData(fieldType),
        isRoot: false,
        isLastChild: false,
        editingDescription: false,
        isNew: this.new
      } as TypeMemberTreeNode
    });
    if (nodes.length > 0) {
      nodes[nodes.length - 1].isLastChild = true;
    }
    return nodes;
  }

  onTypeNameClicked(item: TypeMemberTreeNode) {
    if (this.editable) {
      this.editTypeRequested(item);
    } else {
      this.typeNameClicked.emit(item.type.name);
    }
  }

  editTypeRequested(item: TypeMemberTreeNode) {
    const dialog: MatDialogRef<TypeSearchContainerComponent, TypeSelectedEvent> = this.dialog.open(TypeSearchContainerComponent, {
      height: '80vh',
      width: '1600px',
      maxWidth: '80vw'
    });
    dialog.afterClosed().subscribe(result => {
      if (!isNullOrUndefined(result)) {
        const resultType = result.type;
        item.type = resultType;
        this.member.type = resultType.name;
        this.emitUpdateIfRequired();
        if (result.source === 'new') {
          this.newTypeCreated.emit(result.type);
        }
      }
    })
  }
}

class TreeLoader implements TuiTreeLoader<TypeMemberTreeNode> {
  hasChildren(item: TypeMemberTreeNode): boolean {
    return item.children.length > 0;
  }

  loadChildren(item: TypeMemberTreeNode): Observable<TypeMemberTreeNode[]> {
    return of(item.children)
  }

}

import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { Field, findType, PartialSchema, QualifiedName, Schema, Type } from '../../services/schema';
import { isNullOrUndefined } from 'src/app/utils/utils';
import {EMPTY_ARRAY, TuiHandler} from '@taiga-ui/cdk';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { TypeSearchContainerComponent } from '../type-search/type-search-container.component';
import { BaseDeferredEditComponent } from '../base-deferred-edit.component';
import { TypeSelectedEvent } from 'src/app/type-viewer/type-search/type-selected-event';
import { ChangeFieldTypeEvent } from '../../project-import/schema-importer.service';


export interface TypeMemberTreeNode {
  name: string;
  type: Type;
  parentModel: Type;
  field: Field;
  isNew: boolean;
  children: TypeMemberTreeNode[];
  isRoot: boolean;
  isLastChild: boolean;
}

@Component({
  selector: 'app-model-member',
  template: `
      <tui-tree
        [tuiTreeController]="false"
        [value]="treeData"
        [content]="treeContent"
        [childrenHandler]="treeChildrenHandler"
      ></tui-tree>
      <ng-template #treeContent let-item>
        <div class="tree-node" [ngClass]="{child: !item.isRoot, isLastChild: item.isLastChild}">
          <app-model-member-tree-node [treeNode]="item"
                                      [editable]="editable && item.isRoot"
                                      [schemaMemberNavigable]="schemaMemberNavigable"
                                      [showFullTypeNames]="showFullTypeNames"
                                      (nodeUpdated)="updateDeferred.emit({schemaEditOperation: $event, member})"
                                      (typeNameClicked)="onTypeNameClicked(item)"
          ></app-model-member-tree-node>
        </div>
      </ng-template>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./model-member.component.scss']
})
export class ModelMemberComponent extends BaseDeferredEditComponent<Field> {

  constructor(private dialog: MatDialog) {
    super();
  }

  private _member: Field;

  @Input()
  showFullTypeNames = false;

  @Input()
  editable: boolean = false;

  @Input()
  schemaMemberNavigable: boolean = false;

  @Output()
  typeNameClicked = new EventEmitter<QualifiedName>()

  @Input()
  new: boolean = false;

  @Input()
  parentModel: Type;

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

  @Input()
  partialSchema: PartialSchema;

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

  treeData: TypeMemberTreeNode

  private setMemberType() {
    if (isNullOrUndefined(this.schema) || isNullOrUndefined(this.member) || isNullOrUndefined(this.anonymousTypes)) {
      return;
    }
    this.memberType = findType(this.schema, this.member.type.parameterizedName, this.anonymousTypes);
    this.new = this.anonymousTypes.includes(this.memberType);
    this.treeData = this.buildTreeRootNode();
  }

  private readonly loadedChildren = new Set<TypeMemberTreeNode>();

  protected readonly treeChildrenHandler: TuiHandler<TypeMemberTreeNode, readonly TypeMemberTreeNode[]> = (item) =>
    item.children || EMPTY_ARRAY;

  private buildTreeRootNode(): TypeMemberTreeNode {
    return {
      name: this.memberName,
      field: this.member,
      type: this.memberType,
      parentModel: this.parentModel,
      children: this.buildTreeData(this.memberType),
      isRoot: true,
      isLastChild: !this.memberType.isScalar,
      isNew: this.new
    }
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
        isNew: this.new
      } as TypeMemberTreeNode
    });
    if (nodes.length > 0) {
      nodes[nodes.length - 1].isLastChild = true;
    }
    return nodes;
  }

  onTypeNameClicked(item: TypeMemberTreeNode) {
    // Only allow root nodes to be editable for now
    if (this.editable && item.isRoot) {
      this.editTypeRequested(item);
    } else if (this.schemaMemberNavigable) {
      this.typeNameClicked.emit(item.type.name);
    }
  }

  editTypeRequested(item: TypeMemberTreeNode) {
    const dialog: MatDialogRef<TypeSearchContainerComponent, TypeSelectedEvent> = this.dialog.open(TypeSearchContainerComponent, {
      height: '80vh',
      width: '1600px',
      maxWidth: '80vw',
      data: {
        partialSchema: this.partialSchema,
        parentModel: this.parentModel
      }
    });
    dialog.afterClosed().subscribe(result => {
      if (!isNullOrUndefined(result)) {
        const eventToEmit: ChangeFieldTypeEvent = {
          editKind: 'ChangeFieldType',
          symbol: this.parentModel.memberQualifiedName,
          fieldName: this.memberName,
          newReturnType: result.type.name,
          nullable: false // TODO: what should this be? Do we default to required fields?
        }
        const resultType = result.type;
        item.type = resultType;
        this.member.type = resultType.name;
        // Need this to force an Angular update, ideally we'd be passing in immutable data
        this.member = JSON.parse(JSON.stringify(this.member));
        this.emitUpdateIfRequired(eventToEmit);
        if (result.source === 'new') {
          this.newTypeCreated.emit(result.type);
        }
      }
    })
  }
}

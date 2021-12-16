import {Component, Input} from '@angular/core';
import {Field, findType, Schema, Type} from '../services/schema';
import {isNullOrUndefined} from 'util';
import {TuiHandler} from '@taiga-ui/cdk';


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
    <div  [tuiTreeController]="true">
      <tui-tree
        *ngFor="let item of treeData"
        [tuiTreeController]="true"
        [value]="item"
        [content]="treeContent"
        [childrenHandler]="treeChildrenHandler"
      ></tui-tree>
      <ng-template #treeContent let-item>
        <div class="tree-node" [ngClass]="{child: !item.isRoot, isLastChild: item.isLastChild}">
          <app-model-member-tree-node [treeNode]="item" [editable]="editable"
          ></app-model-member-tree-node>
        </div>

      </ng-template>

    </div>


  `,
  styleUrls: ['./model-member.component.scss']
})
export class ModelMemberComponent {

  private _member: Field;

  @Input()
  editable: boolean = false;

  descriptionEditable = false;

  @Input()
  new: boolean = false;
  treeData: TypeMemberTreeNode[];

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
  memberName: string;

  memberType: Type

  private setMemberType() {
    if (isNullOrUndefined(this.schema) || isNullOrUndefined(this.member)) {
      return;
    }
    this.memberType = findType(this.schema, this.member.type.parameterizedName);
    this.treeData = [this.buildTreeRootNode()];
  }

  makeDescriptionEditable(item:TypeMemberTreeNode, editable: boolean) {
    if (this.editable) {
      item.editingDescription = editable;
    }
  }


  treeChildrenHandler: TuiHandler<TypeMemberTreeNode, TypeMemberTreeNode[]> = item => item.children

  private buildTreeRootNode(): TypeMemberTreeNode {
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
  }

  private buildTreeData(memberType: Type): TypeMemberTreeNode[] {
    const nodes = Object.keys(memberType.attributes).map(key => {
      const field = memberType.attributes[key];
      const fieldType = findType(this.schema, memberType.attributes[key].type.parameterizedName);
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
}

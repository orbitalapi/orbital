import {Component, Input} from '@angular/core';
import {
  BaseTypedInstanceViewer,
  getTypedObjectAttribute,
  getTypedObjectAttributeValue
} from './BaseTypedInstanceViewer';
import {isNullOrUndefined} from 'util';
import {
  Field, findType, getCollectionMemberType,
  InstanceLike, InstanceLikeOrCollection,
  isTypedInstance,
  isUntypedInstance,
  Type, TypedInstance,
  UnknownType,
  UntypedInstance
} from '../services/schema';
import {InstanceSelectedEvent} from '../query-panel/instance-selected-event';
import {isValueWithTypeName, ValueWithTypeName} from '../services/models';
import {TuiHandler} from '@taiga-ui/cdk';
import {TypeMemberTreeNode} from '../type-viewer/model-attribute-tree-list/model-member.component';
import {isArray} from 'angular';

/**
 * This displays results fetched from service calls.
 * The results are generally returned either with Type information attached
 * (verbose mode), or without (simple mode).
 * We want a single component for displaying both of these types, so theres
 * a bit of gymnastics involved to encapsulate both use cases.
 * Previously, this was split across multiple different UI components, but that
 * created too many inconsistencies in display.
 */

export interface ResultTreeMember {
  fieldName: string | null;
  value: any;
  type: Type;
  children: ResultTreeMember[];
  path: string;
  instance: InstanceLike;
  rootResultInstance: ValueWithTypeName;
}

@Component({
  selector: 'app-object-view',
  templateUrl: './object-view.component.html',
  styleUrls: ['./object-view.component.scss']
})
export class ObjectViewComponent extends BaseTypedInstanceViewer {

  NOT_PROVIDED = 'Value not provided';

  @Input()
    // eslint-disable-next-line @typescript-eslint/no-inferrable-types
  selectable: boolean = false;



  onAttributeClicked(member: ResultTreeMember) {
    /**
     * When the root node is a collection, we can end up with some junk
     * in the path.
     * Collection indices aren't valid as the root, since that's the role of the valueId
     * on the TypeNamedInstance
     */
    function trimPath(candidatePath: string): string {
      // If the path starts with a '.', drop it off
      if (candidatePath.startsWith('.')) {
        return trimPath(candidatePath.substr(1));
      }

      // If the path starts with an array index -- [0] -- then drop that off
      if (candidatePath.startsWith('[')) {
        const parts: string[] = candidatePath.split('.');
        const trimmedPath = parts.slice(1).join('.'); // Drop the first member in the array, since it's an array selector [0].
        return trimPath(trimmedPath);
      }
      return candidatePath;
    }

    const selectedAttributePath = trimPath(member.path);
    if (this.selectable) {
      const instance = member.instance;
      let instanceValue: InstanceLike | UntypedInstance;
      if (!isTypedInstance(instance) && !isUntypedInstance(instance)) {
        // If we only have the scalar attribute value, then wrap it into an untyped instance,
        // so that we can display lineage correctly later
        instanceValue = {
          value: instance,
          type: UnknownType.UnknownType
        } as UntypedInstance;
      } else {
        instanceValue = instance;
      }
      this.instanceClicked.emit(
        new InstanceSelectedEvent(instanceValue, null, member.rootResultInstance.valueId, selectedAttributePath, member.rootResultInstance.queryId));
    }
  }
  onReady() {
    const rootResultInstanceOrNull = (isValueWithTypeName(this.instance)) ? this.instance : null;
    let treeData = this.buildTreeData(this.instance, this.type, null, '', rootResultInstanceOrNull);
    this.treeData = Array.isArray(treeData) ? treeData : [treeData];
  }

  treeData: ResultTreeMember[] = [];

  treeChildrenHandler: TuiHandler<ResultTreeMember, ResultTreeMember[]> = item => Array.isArray(item) ? item : item.children;

  private buildTreeData(instance: InstanceLikeOrCollection, type: Type, fieldName: string = null, path: string = '', rootResultInstance: ValueWithTypeName | null = null): ResultTreeMember {
    if (Array.isArray(instance)) {
      const members = instance.map((value, index) => {
        let derivedRoot:ValueWithTypeName = rootResultInstance;
        if (isNullOrUndefined(derivedRoot) && isValueWithTypeName(value)) {
          derivedRoot = value;
        }

        const builtArray = this.buildTreeData(value, getCollectionMemberType(type, this.schema, type, this.anonymousTypes), index.toString(), path + '.[' + index + ']', derivedRoot);
        return builtArray;
      }) as ResultTreeMember[];
      return {
        children: members,
        path: path,
        value:'',
        type: type,
        rootResultInstance: rootResultInstance,
        fieldName: fieldName,
        instance: null // TODO : Should we modify the interface to accept InstanceLike | InstanceLike[] ?
      } as ResultTreeMember;
    } else {
      const instanceLike = instance as InstanceLike;
      let children: ResultTreeMember[];
      if (type.isScalar) {
        children = null;
      } else {
        children = Object.keys(type.attributes).map(attributeName => {
          const fieldType = findType(this.schema, type.attributes[attributeName].type.parameterizedName, this.anonymousTypes);
          const fieldValue = getTypedObjectAttribute(instance, attributeName);
          return this.buildTreeData(fieldValue, fieldType, attributeName, path + '.' + attributeName, rootResultInstance)
        }) as ResultTreeMember[]; // TODO : This cast isn't correct
      }
      let value;
      if (isNullOrUndefined(instance)) {
        value = null
      } else if (!isScalar(instance.value || instance)) { // This is the parent of an object.  Use an empty string, so the tree can be expanded
        value = '';
      } else {
        value = instance.value || instance;
      }
      // find the longest label we need
      const member = {
        value: value,
        type: type,
        fieldName: fieldName,
        children: children,
        path: path,
        instance: instanceLike,
        rootResultInstance: rootResultInstance
      } as ResultTreeMember;
      if (member.value?.toString() === '[object Object]') {
        debugger;
      }
      return member;
    }
  }

  // Just a typing hack
  treeNode = (item) => item as ResultTreeMember;
}

function isScalar(value): boolean {
  return typeof (value) !== 'object';
}

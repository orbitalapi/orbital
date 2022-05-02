import {Component, Input} from '@angular/core';
import {BaseTypedInstanceViewer} from './BaseTypedInstanceViewer';
import {isNullOrUndefined} from 'util';
import {
  InstanceLike,
  InstanceLikeOrCollection,
  isTypedInstance,
  isUntypedInstance,
  UnknownType,
  UntypedInstance
} from '../services/schema';
import {InstanceSelectedEvent} from '../query-panel/instance-selected-event';
import {isValueWithTypeName, ValueWithTypeName} from '../services/models';
import {TuiHandler} from '@taiga-ui/cdk';
import {Observable, Subscription} from "rxjs";


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
  children: ResultTreeMember[];
  path: string;
  instance: InstanceLike;
  rootResultInstance: ValueWithTypeName;
}

@Component({
  selector: 'app-object-view',
  styleUrls: ['./object-view.component.scss'],
  template: `
    <tui-pagination
      *ngIf="treeDataPages.length > 1"
      [length]="treeDataPages.length"
      [(index)]="treeDataCurrentPage"
    ></tui-pagination>
    <tui-tree
      [value]="treeDataPages[treeDataCurrentPage]"
      [tuiTreeController]="true"
      [content]="treeContent"
      [childrenHandler]="treeChildrenHandler"></tui-tree>
    <ng-template #treeContent let-item>
      <div class="tree-node">
        <div *ngIf="treeNode(item).fieldName" class="field-name">{{treeNode(item).fieldName}}</div>
        <div class="field-value" [class.selectable]="selectable"
             (click)="onAttributeClicked(item)">{{treeNode(item).value}}</div>
      </div>
    </ng-template>
  `
})
export class ObjectViewComponent extends BaseTypedInstanceViewer {


  NOT_PROVIDED = 'Value not provided';

  @Input()
    // eslint-disable-next-line @typescript-eslint/no-inferrable-types
  selectable: boolean = false;

  private instanceUpdateSubscription: Subscription;
  private _instances$: Observable<InstanceLike>
  @Input()
  get instances$(): Observable<InstanceLike> {
    return this._instances$;
  }

  set instances$(value: Observable<InstanceLike>) {
    if (value === this._instances$) {
      return;
    }
    if (this.instanceUpdateSubscription) {
      this.instanceUpdateSubscription.unsubscribe();
    }
    this._instances$ = value;
    // Set the instance to an array, as we'll be appending to it as we receive items from the subscription
    this.instance = [];
    if (isNullOrUndefined(this.instances$)) {
      return;
    }

    // Will subscribe to the observable only when all other properties have been set too.
    this.checkIfReady();
  }


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

  private subscribeForUpdates(source: Observable<InstanceLike>) {
    this.treeData = [];
    this.treeDataPages = [];
    this.treeDataCurrentPage = 0;

    // Set the protected member, to avoid triggering another
    // checkIfReady() loop - chances are we're already inside of one.
    this._instance = [];
    const pageSize = 20;

    this.instanceUpdateSubscription = source.subscribe(instance => {
      const instanceArray = this.instance as InstanceLike[];
      const newLength = instanceArray.push(instance)
      const pageNumber = Math.floor(newLength / pageSize)
      const page = this.treeDataPages[pageNumber] || (this.treeDataPages[pageNumber] = []);
      const label = newLength.toString();// Use the index as the label
      const thisInstanceAsResultTree = this.buildTreeData(instance, label, '', instance as ValueWithTypeName)
      page.push(thisInstanceAsResultTree)
    })
  }

  onReady() {
    if (this.instances$) {
      this.subscribeForUpdates(this.instances$);
    }
  }

  treeData: ResultTreeMember[] = [];
  treeDataPages: ResultTreeMember[][] = [];
  treeDataCurrentPage: number = 0;

  treeChildrenHandler: TuiHandler<ResultTreeMember, ResultTreeMember[]> = item => Array.isArray(item) ? item : item.children;

  private buildTreeData(instance: InstanceLikeOrCollection, fieldName: string = null, path: string = '', rootResultInstance: ValueWithTypeName | null = null): ResultTreeMember {
    if (Array.isArray(instance)) {
      const members = instance.map((value, index) => {
        let derivedRoot: ValueWithTypeName = rootResultInstance;
        if (isNullOrUndefined(derivedRoot) && isValueWithTypeName(value)) {
          derivedRoot = value;
        }

        const builtArray = this.buildTreeData(value, index.toString(), path + '.[' + index + ']', derivedRoot);
        return builtArray;
      }) as ResultTreeMember[];
      return {
        children: members,
        path: path,
        value: '',
        rootResultInstance: rootResultInstance,
        fieldName: fieldName,
        instance: null // TODO : Should we modify the interface to accept InstanceLike | InstanceLike[] ?
      } as ResultTreeMember;
    } else {
      const instanceLike = instance as InstanceLike;
      let children: ResultTreeMember[];

      // Design choice:
      // Previously, we leveraged the type here to find the attributes and
      // determine what to iterate.
      // However, that breaks (or causes annoying race conditions) if we're using an anonymous type.
      // So, instead just iterate the attributes of the value directly.
      const itemValue = isValueWithTypeName(instance) ? instance.value : instance;
      const scalar = isScalar(itemValue)
      if (scalar) {
        children = null;
      } else {
        const attributeNames = Object.keys(itemValue)
        children = attributeNames.map(attributeName => {
          const fieldValue = itemValue[attributeName];
          return this.buildTreeData(fieldValue, attributeName, path + '.' + attributeName, rootResultInstance)
        }) as ResultTreeMember[]; // TODO : This cast isn't correct
      }
      let value;
      if (isNullOrUndefined(instance)) {
        value = null
      } else if (!scalar) { // This is the parent of an object.  Use an empty string, so the tree can be expanded
        value = '';
      } else {
        value = instance.value || instance;
      }
      // find the longest label we need
      const member = {
        value: value,
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

export function isScalar(value): boolean {
  return typeof (value) !== 'object';
}

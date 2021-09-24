import {Component, Input} from '@angular/core';
import {BaseTypedInstanceViewer} from './BaseTypedInstanceViewer';
import {isNullOrUndefined} from 'util';
import {InstanceLike, isTypedInstance, isUntypedInstance, UnknownType, UntypedInstance} from '../services/schema';
import {InstanceSelectedEvent} from '../query-panel/instance-selected-event';
import {ValueWithTypeName} from '../services/models';

/**
 * This displays results fetched from service calls.
 * The results are generally returned either with Type information attached
 * (verbose mode), or without (simple mode).
 * We want a single component for displaying both of these types, so theres
 * a bit of gymnastics involved to encapsulate both use cases.
 * Previously, this was split across multiple different UI components, but that
 * created too many inconsistencies in display.
 */

@Component({
  selector: 'app-object-view',
  templateUrl: './object-view.component.html',
  styleUrls: ['./object-view.component.scss']
})
export class ObjectViewComponent extends BaseTypedInstanceViewer {

  NOT_PROVIDED = 'Value not provided';

  @Input()
  path = '';

  @Input()
  topLevel = true;

  @Input()
    // tslint:disable-next-line:no-inferrable-types
  selectable: boolean = false;

  @Input()
  rootInstance: ValueWithTypeName;

  get isScalar(): boolean {
    if (isNullOrUndefined(this.instance) || isNullOrUndefined(this.type === null)) {
      return false;
    } else {
      return this.type.isScalar;
    }
  }

  get scalarValue(): any | null {
    if (!this.isScalar) {
      return null;
    }
    // HACK :  This needs investigation.
    // When performing a query that returns a scalar value,
    // it looks like the value passed here is not a typed object, but just
    // the value itself.
    // if (isTypedInstance(this.typedObject)) {
    //   return this.typedObject.value;
    // } else {
    //   return this.typedObject;
    // }
    return this.instance;

  }

  get isTypedObject(): boolean {
    if (!this.type) {
      return false;
    }
    return !this.type.isScalar;
    // this.result.hasOwnProperty("type")
    // && (<any>this.result).type.hasOwnProperty("fullyQualifiedName")
  }


  onAttributeClicked(attributeName: string) {
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

    const selectedAttributePath = trimPath(this.path + '.' + attributeName);
    if (this.selectable) {
      const instance = this.getTypedObjectAttribute(attributeName);
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
        new InstanceSelectedEvent(instanceValue, null, this.rootInstance.valueId, selectedAttributePath, this.rootInstance.queryId));
    }
  }

  onTopLevelPrimitiveClicked() {
    // Not re ally sure how to resolve this, so not trying right now.

    // if (this.selectable) {
    //   const nodeId = null; // todo
    //   // This casting probably won't work, need to revisit once this is rendering agian
    //   this.instanceClicked.emit(new InstanceSelectedEvent(
    //     this.instance as InstanceLike, null, nodeId));
    // }

  }
}


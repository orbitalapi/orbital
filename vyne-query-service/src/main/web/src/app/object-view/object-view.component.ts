import {Component, Input} from '@angular/core';
import {BaseTypedInstanceViewer} from './BaseTypedInstanceViewer';
import {isNullOrUndefined} from 'util';
import {InstanceLike, isTypedInstance} from '../services/schema';
import {InstanceSelectedEvent} from '../query-panel/instance-selected-event';

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
  topLevel = true;

  @Input()
    // tslint:disable-next-line:no-inferrable-types
  selectable: boolean = false;

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
    // if (this.selectable) {
    //   const nodeId = null; // todo
    //   const instance = this.getTypedObjectAttribute(attributeName);
    //   this.instanceClicked.emit(new InstanceSelectedEvent(instance, null, this));
    //
    // }
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


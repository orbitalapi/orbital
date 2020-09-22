import {Component, EventEmitter, Input, Output} from '@angular/core';
import {isTypedInstance, isTypeNamedInstance, TypeNamedInstance} from '../services/query.service';
import {Field, findType, getCollectionMemberType, Schema, Type, TypedInstance} from '../services/schema';
import {BaseTypedInstanceViewer} from './BaseTypedInstanceViewer';
import {MatRadioChange} from '@angular/material/radio';

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

  @Output() downloadParsedDataClicked = new EventEmitter<Boolean>();

  @Output() downloadTypedParsedDataClicked = new EventEmitter<Boolean>();


  // Indicates if it's a straight typedInstance (ie., a typedValue)
  // or a typed object, which is indexed with property names

  isTypeInfoIncluded = false;

  get isPrimitive(): boolean {
    return this._instance != null && this.typedObject.value != null && !this.isTypedObject && !this.isArray;
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
    if (this.selectable) {
      const instance = this.getTypedObjectAttribute(attributeName);
      this.instanceClicked.emit(instance);

    }
  }

  onTopLevelPrimitiveClicked() {
    if (this.selectable) {
      this.instanceClicked.emit(this.typedObject);
    }

  }

  downloadParsedData() {
    if (this.isTypeInfoIncluded) {
      this.downloadTypedParsedDataClicked.emit();
    } else {
      this.downloadParsedDataClicked.emit();
    }
  }

  handleRadio($event: MatRadioChange) {
    this.isTypeInfoIncluded = $event.value === 'TypeIncluded' ? true : false;
  }
}

export type InstanceLike = TypedInstance | TypedObjectAttributes | TypeNamedInstance;
export type InstanceLikeOrCollection = InstanceLike | InstanceLike[];
export type TypeInstanceOrAttributeSet = TypedInstance | TypedObjectAttributes;

export interface TypedObjectAttributes {
  [key: string]: TypeInstanceOrAttributeSet;
}

export function typeName(instance: InstanceLike): string {
  if (isTypedInstance(instance)) {
    return instance.type.name.fullyQualifiedName;
  } else if (isTypeNamedInstance(instance)) {
    return instance.typeName;
  } else {
    // No good reason for not supporting this, just haven't hit the usecase yet, and it's not
    // obvious how we should support it.
    throw new Error('Looks like the instance is a TypedObjectAttributes, which isn\'t yet supported');
  }
}


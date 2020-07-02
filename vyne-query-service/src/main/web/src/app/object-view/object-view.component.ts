import {Component, EventEmitter, Input, Output} from '@angular/core';
import {isTypedInstance, isTypeNamedInstance, TypeNamedInstance} from '../services/query.service';
import {Field, findType, getCollectionMemberType, Schema, Type, TypedInstance} from '../services/schema';

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
export class ObjectViewComponent {
  private _instance: InstanceLikeOrCollection;
  NOT_PROVIDED = 'Value not provided';

  @Output()
  instanceClicked = new EventEmitter<InstanceLike>();

  @Input()
  schema: Schema;

  @Input()
  topLevel = true;

  @Input()
    // tslint:disable-next-line:no-inferrable-types
  selectable: boolean = false;

  private fieldTypes = new Map<Field, Type>();
  private _type: Type;
  private _derivedType: Type;
  private _collectionMemberType: Type;

  @Input()
  get instance(): InstanceLikeOrCollection {
    return this._instance;
  }

  set instance(value: InstanceLikeOrCollection) {
    this._instance = value;
    // When the instance changes, any assumptions we've made about
    // types based on the old instance are invalid, so null out to recompute
    this._derivedType = null;
    this._collectionMemberType = null;
  }

  @Input()
  get type(): Type {
    if (this._type) {
      return this._type;
    }
    this._derivedType = this.selectType(this._instance);
    return this._derivedType;
  }

  private selectType(instance: InstanceLikeOrCollection): Type {
    if (Array.isArray(instance)) {
      return this.selectType(instance[0]);
    }
    if (instance && isTypedInstance(instance)) {
      return instance.type;
    }
    if (instance && isTypeNamedInstance(instance)) {
      return findType(this.schema, instance.typeName);
    }
    console.error('No scenario for finding a type -- returning null');
    return null;
  }

  set type(value: Type) {
    this._type = value;
    this._collectionMemberType = null;
    this._derivedType = null;
  }


  get typedObject(): TypeNamedInstance {
    return <TypeNamedInstance>this._instance;
  }

  get typedObjectAttributeNames(): string[] {
    if (!this.type || this.isArray) {
      return [];
    }
    return Object.keys(this.type.attributes);
  }

  getTypedObjectAttributeValue(name: string): any {
    if (this._instance === undefined || this._instance === null) {
      return null;
    }
    const isScalar = this.getTypeForAttribute(name).isScalar;
    const attributeValue = this.getTypedObjectAttribute(name);
    if (attributeValue === undefined) {
      return null;
    }
    if (isTypedInstance(this._instance)) {
      if (isScalar) {
        return attributeValue;
      } else {
        // NO particular reason for this, just haven't hit this code path yet
        throw new Error('This is unhandled - non scalar TypedInstance');
      }
    } else if (isTypeNamedInstance(this._instance)) {
      if (isScalar) {
        return (attributeValue as TypeNamedInstance).value;
      } else {
        // NO particular reason for this, just haven't hit this code path yet
        throw new Error('This is unhandled - non scalar TypeNamedInstance');
      }
    } else if (typeof this._instance === 'object' && isScalar) {
      return this._instance[name];
    }


  }

  getTypedObjectAttribute(name: string): InstanceLike {
    if (this.isArray) {
      return null;
    }
    const instance = this._instance as InstanceLike;
    if (!instance) {
      return null;
    }
    if (isTypedInstance(instance)) {
      return instance.value[name];
    } else if (isTypeNamedInstance(instance)) {
      return instance.value[name];
    } else { // TypedObjectAttributes
      return (instance as TypedObjectAttributes)[name];
    }
  }


  getTypeForAttribute(attributeName: string): Type {
    const field: Field = this.type.attributes[attributeName];
    if (this.fieldTypes.has(field)) {
      return this.fieldTypes.get(field);
    } else {
      const fieldType = findType(this.schema, field.type.parameterizedName);
      this.fieldTypes.set(field, fieldType);
      return fieldType;
    }
  }


  // Indicates if it's a straight typedInstance (ie., a typedValue)
  // or a typed object, which is indexed with property names

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

  get isArray(): boolean {
    return this._instance != null &&
      this._instance.constructor === Array;
  }

  get collectionMemberType(): Type {
    if (!this.isArray) {
      return null;
    }
    if (this._collectionMemberType === null || this._collectionMemberType === undefined) {
      this._collectionMemberType = getCollectionMemberType(this.type, this.schema);
    }
    return this._collectionMemberType;
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


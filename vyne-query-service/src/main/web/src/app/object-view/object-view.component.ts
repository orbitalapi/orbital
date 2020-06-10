import {Component, Input} from '@angular/core';
import {TypeNamedInstance} from '../services/query.service';
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
  NOT_PROVIDED = 'Value not provided';
  @Input()
  instance: InstanceLikeOrCollection;

  @Input()
  schema: Schema;

  @Input()
  topLevel = true;

  private fieldTypes = new Map<Field, Type>();
  private _type: Type;

  @Input()
  get type(): Type {
    if (this._type) {
      return this._type;
    }
    return this.selectType(this.instance);
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
  }


  get typedObject(): TypeNamedInstance {
    return <TypeNamedInstance>this.instance;
  }

  get typedObjectAttributeNames(): string[] {
    if (!this.type || this.isArray) {
      return [];
    }
    return Object.keys(this.type.attributes);
  }

  getTypedObjectAttributeValue(name: string): any {
    if (this.instance === undefined || this.instance === null) {
      return null;
    }
    const isScalar = this.getTypeForAttribute(name).isScalar;
    const attributeValue = this.getTypedObjectAttribute(name);
    if (attributeValue === undefined) {
      return null;
    }
    if (isTypedInstance(this.instance)) {
      if (isScalar) {
        return attributeValue;
      } else {
        // NO particular reason for this, just haven't hit this code path yet
        throw new Error('This is unhandled - non scalar TypedInstance');
      }
    } else if (isTypeNamedInstance(this.instance)) {
      if (isScalar) {
        return (attributeValue as TypeNamedInstance).value;
      } else {
        // NO particular reason for this, just haven't hit this code path yet
        throw new Error('This is unhandled - non scalar TypeNamedInstance');
      }
    } else if (typeof this.instance === 'object' && isScalar) {
      return this.instance[name];
    }


  }

  getTypedObjectAttribute(name: string): InstanceLike {
    if (this.isArray) {
      return null;
    }
    const instance = this.instance as InstanceLike;
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
    return this.instance != null && this.typedObject.value != null && !this.isTypedObject && !this.isArray;
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
    return this.instance != null &&
      this.instance.constructor === Array;
  }

  get collectionMemberType(): Type {
    if (!this.isArray) {
      return null;
    }
    return getCollectionMemberType(this.type, this.schema);
  }
}

export type InstanceLike = TypedInstance | TypedObjectAttributes | TypeNamedInstance;
export type InstanceLikeOrCollection = InstanceLike | InstanceLike[];
export type TypeInstanceOrAttributeSet = TypedInstance | TypedObjectAttributes;

export interface TypedObjectAttributes {
  [key: string]: TypeInstanceOrAttributeSet;
}

function isTypedInstance(instance: InstanceLikeOrCollection): instance is TypedInstance {
  const instanceAny = instance as any;
  return instanceAny.type !== undefined && instanceAny.value !== undefined;
}

function isTypeNamedInstance(instance: InstanceLikeOrCollection): instance is TypeNamedInstance {
  const instanceAny = instance as any;
  return instanceAny.typeName !== undefined && instanceAny.value !== undefined;
}

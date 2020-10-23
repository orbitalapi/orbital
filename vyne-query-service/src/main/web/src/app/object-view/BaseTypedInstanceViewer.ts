import {InstanceLike, InstanceLikeOrCollection, TypedObjectAttributes} from './object-view.component';
import {EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {Field, findType, getCollectionMemberType, Schema, Type} from '../services/schema';
import {isTypedInstance, isTypeNamedInstance, TypeNamedInstance} from '../services/query.service';


export class BaseTypedInstanceViewer implements OnInit, OnDestroy {
  private componentId = Math.random().toString(36).substring(7);
  protected _instance: InstanceLikeOrCollection;

  @Output()
  instanceClicked = new EventEmitter<InstanceLike>();

  @Input()
  schema: Schema;

  protected fieldTypes = new Map<Field, Type>();
  protected _type: Type;
  protected _derivedType: Type;
  protected _collectionMemberType: Type;


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


  get typedObject(): TypeNamedInstance {
    return <TypeNamedInstance>this._instance;
  }

  get typedObjectAttributeNames(): string[] {
    if (!this.type || this.isArray) {
      return [];
    }
    return Object.keys(this.type.attributes);
  }


  @Input()
  get type(): Type {
    if (this._type) {
      return this._type;
    }
    if (this._derivedType) {
      return this._derivedType;
    }
    if (!this._instance) {
      return null;
    } else {
      this._derivedType = this.selectType(this._instance);
      return this._derivedType;
    }
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
    // Don't use this.type directly here, as sometimes we're actually working against
    // the arrayType (ie., when in a table view)
    const type = (this.isArray) ? this.collectionMemberType : this.type;
    const field: Field = type.attributes[attributeName];
    if (this.fieldTypes.has(field)) {
      return this.fieldTypes.get(field);
    } else {
      const fieldType = findType(this.schema, field.type.parameterizedName);
      this.fieldTypes.set(field, fieldType);
      return fieldType;
    }
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

  ngOnDestroy(): void {
    console.log(`viewer ${this.componentId} destroyed`);
  }

  ngOnInit(): void {
    console.log(`viewer ${this.componentId} initialized`);
  }

}

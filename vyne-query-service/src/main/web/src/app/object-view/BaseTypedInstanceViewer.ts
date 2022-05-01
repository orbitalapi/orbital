import {Directive, EventEmitter, Input, Output} from '@angular/core';
import {
  Field,
  findType,
  getCollectionMemberType,
  InstanceLike,
  InstanceLikeOrCollection,
  isTypedInstance,
  isTypeNamedInstance,
  Schema,
  Type,
  TypedObjectAttributes,
  TypeNamedInstance
} from '../services/schema';
import {InstanceSelectedEvent} from '../query-panel/instance-selected-event';
import {isNullOrUndefined} from 'util';
import {isValueWithTypeName} from '../services/models';
import {ComponentWithSubscriptions} from '../utils/component-with-subscriptions';


@Directive()
export class BaseTypedInstanceViewer extends ComponentWithSubscriptions {
  private componentId = Math.random().toString(36).substring(7);

  @Output()
  instanceClicked = new EventEmitter<InstanceSelectedEvent>();

  private _schema: Schema;


  @Input()
  public get schema(): Schema {
    return this._schema;
  }

  public set schema(value: Schema) {
    if (this.schema === value) {
      return;
    }
    this._schema = value;
    this.checkIfReady();
    this.onSchemaChanged();
  }

  protected _instance: InstanceLikeOrCollection;
  @Input()
  get instance(): InstanceLikeOrCollection {
    return this._instance;
  }

  set instance(value) {
    if (this._instance === value) {
      return;
    }
    this._instance = value;
    this.checkIfReady();
  }


  protected fieldTypes = new Map<Field, Type>();
  protected _type: Type;
  protected _derivedType: Type;
  protected _collectionMemberType: Type;


  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    this._type = value;
    this.checkIfReady();

    // this._collectionMemberType = null;
    // this._derivedType = null;
  }

  @Input()
  anonymousTypes: Type[] = [];

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
    } else if (isValueWithTypeName(this.instance)) { // TODO : Should there be an isScalar check here?
      return this.instance.value[name];
    } else if (isTypeNamedInstance(this.instance)) {
      // IF we see this log message, work out where we're getting the instances from.
      console.log('Received a typeNamedInstance ... thought these were deprecated?!');
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
    // Don't use this.type directly here, as sometimes we're actually working against
    // the arrayType (ie., when in a table view)
    const type = (this.isArray) ? this.collectionMemberType : this.type;
    const field: Field = type.attributes[attributeName];
    if (this.fieldTypes.has(field)) {
      return this.fieldTypes.get(field);
    } else {
      const fieldType = findType(this.schema, field.type.parameterizedName, this.anonymousTypes);
      this.fieldTypes.set(field, fieldType);
      return fieldType;
    }
  }


  get isArray(): boolean {
    return this.instance != null &&
      this.instance.constructor === Array;
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

  protected onSchemaChanged() {
    this._collectionMemberType = null;
    if (!isNullOrUndefined(this._type)) {
      this._type = findType(this.schema, this._type.name.parameterizedName, this.anonymousTypes);
    }
    this.checkIfReady();
    // if (!isNullOrUndefined(this._derivedType) && !isNullOrUndefined(this.instance)) {
    //   this._derivedType = this.selectType(this.instance);
    // }
  }

  /**
   * Called with the schema, type and instance have all been set, and on subsequent changes
   * @protected
   */
  protected onReady() {
  }

  protected checkIfReady() {
    if (!isNullOrUndefined(this.schema) && !isNullOrUndefined(this.instance)) {
      this.onReady();
    }
  }
}

export function getTypedObjectAttribute(instanceLike: InstanceLikeOrCollection, name: string): InstanceLike {
  if (Array.isArray(instanceLike)) {
    return null;
  }
  const instance = instanceLike as InstanceLike;
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

export function unwrapValue(instance: InstanceLike): any {
  if (isNullOrUndefined(instance)) {
    return null;
  }
  if (isTypedInstance(instance)) {
    return instance.value
  }
  if (isValueWithTypeName(instance)) {
    return instance.value;
  }
}


export function getTypedObjectAttributeValue(instance: InstanceLikeOrCollection, name: string): any {
  if (isNullOrUndefined(instance)) {
    return null;
  }
  // const isScalar = this.getTypeForAttribute(name).isScalar;
  const attributeValue = getTypedObjectAttribute(instance, name);
  if (attributeValue === undefined) {
    return null;
  }
  if (isTypedInstance(instance)) {
    return attributeValue;
    // if (isScalar) {
    //   return attributeValue;
    // } else {
    //   NO particular reason for this, just haven't hit this code path yet
    // throw new Error('This is unhandled - non scalar TypedInstance');
    // }
  } else if (isValueWithTypeName(instance)) { // TODO : Should there be an isScalar check here?
    return instance.value[name];
  } else if (isTypeNamedInstance(instance)) {
    // IF we see this log message, work out where we're getting the instances from.
    console.log('Received a typeNamedInstance ... thought these were deprecated?!');
    return (attributeValue as TypeNamedInstance).value;
  } else if (typeof instance === 'object') {
    return instance[name];
  }
}

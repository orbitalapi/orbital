import {EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {
  Field,
  findType,
  getCollectionMemberType,
  InstanceLike,
  InstanceLikeOrCollection, isTypedInstance, isTypeNamedInstance,
  Schema,
  Type, TypedObjectAttributes, TypeNamedInstance
} from '../services/schema';
import {InstanceSelectedEvent} from '../query-panel/instance-selected-event';
import {isNull, isNullOrUndefined} from 'util';


export class BaseTypedInstanceViewer implements OnInit, OnDestroy {
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
    this.onSchemaChanged();
  }

  @Input()
  instance: InstanceLikeOrCollection;

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
    // this._collectionMemberType = null;
    // this._derivedType = null;
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

  ngOnDestroy(): void {
    console.log(`viewer ${this.componentId} destroyed`);
  }

  ngOnInit(): void {
    console.log(`viewer ${this.componentId} initialized`);
  }

  protected onSchemaChanged() {
    this._collectionMemberType = null;
    if (!isNullOrUndefined(this._type)) {
      this._type = findType(this.schema, this._type.name.parameterizedName);
    }
    // if (!isNullOrUndefined(this._derivedType) && !isNullOrUndefined(this.instance)) {
    //   this._derivedType = this.selectType(this.instance);
    // }
  }
}

import {Component, Input, OnInit} from '@angular/core';
import {TypedInstanceOrCollection, TypeNamedInstance} from '../services/query.service';
import {Field, Schema, Type, TypedInstance} from '../services/schema';

@Component({
  selector: 'app-object-view',
  templateUrl: './object-view.component.html',
  styleUrls: ['./object-view.component.scss']
})
export class ObjectViewComponent {


  @Input()
    // result: TypeInstanceOrAttributeSet;
  instance: TypedInstanceOrCollection;

  @Input()
  schema: Schema;

  @Input()
  topLevel = true;

  get typedObject(): TypeNamedInstance {
    return <TypeNamedInstance>this.instance;
  }

  get type(): Type {
    if (this.isArray) {
      return null;
    }
    return this.schema.types.find(type => type.name.fullyQualifiedName === this.typedObject.typeName);
  }

  get typedObjectAttributeNames(): string[] {
    if (!this.type) {
      return [];
    }
    // return Array.from(this.type.attributes.keys())
    return Object.keys(this.type.attributes);
  }

  getTypedObjectAttribute(name: string): TypeNamedInstance {
    return this.typedObject.value[name];
  }

  getTypeForAttribute(attributeName: string): Type {
    const typeRef: Field = this.type.attributes[attributeName];
    return this.schema.types.find(type => type.name.fullyQualifiedName === typeRef.type.fullyQualifiedName);
  }


  // Indicates if it's a straight typedInstance (ie., a typedValue)
  // or a typed object, which is indexed with property names

  get isPrimitive(): boolean {
    return this.instance != null && this.typedObject.value != null && !this.isTypedObject && !this.isArray;
  }

  get isTypedObject(): boolean {
    return this.instance != null &&
      !this.isArray &&
      typeof this.typedObject.value === 'object';
    // this.result.hasOwnProperty("type")
    // && (<any>this.result).type.hasOwnProperty("fullyQualifiedName")
  }

  get isArray(): boolean {
    return this.instance != null &&
      this.instance.constructor === Array;
  }
}

type TypeInstanceOrAttributeSet = TypedInstance | TypedObjectAttributes;

interface TypedObjectAttributes {
  [key: string]: TypeInstanceOrAttributeSet;
}

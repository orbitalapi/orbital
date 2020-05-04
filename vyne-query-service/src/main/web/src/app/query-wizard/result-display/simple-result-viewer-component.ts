import {Component, Input, OnInit} from '@angular/core';
import {Field, Schema, Type, TypedInstance} from '../../services/schema';
import {TypedObjectAttributes, TypeInstanceOrAttributeSet} from '../../object-view/object-view.component';

export class SimpleResultViewerComponent  {

  @Input()
  result: TypeInstanceOrAttributeSet;

  // @Input()
  // schema: Schema;

  // @Input()
  // type: Type;

  // ngOnInit() {
  // }
  //
  // get typedObject(): TypedObjectAttributes {
  //   return <TypedObjectAttributes>this.result;
  // }
  //
  // get typedObjectAttributeNames(): string[] {
  //   if (!this.type) { return []; }
  //   // return Array.from(this.type.attributes.keys())
  //   return Object.keys(this.type.attributes);
  // }
  //
  // getTypedObjectAttribute(name: string): TypeInstanceOrAttributeSet {
  //   return this.typedObject[name];
  // }
  //
  // getTypeForAttribute(attributeName: string): Type {
  //   const typeRef: Field = this.type.attributes[attributeName];
  //   return this.schema.types.find(type => type.name.fullyQualifiedName === typeRef.type.fullyQualifiedName);
  // }
  //
  //
  // // Indicates if it's a straight typedInstance (ie., a typedValue)
  // // or a typed object, which is indexed with property names
  //
  // get isPrimitive(): boolean {
  //   return !this.isTypedObject && !this.isArray;
  // }
  //
  // get isTypedObject(): boolean {
  //   return this.result != null &&
  //     !this.isArray &&
  //     typeof this.result === 'object';
  // }
  //
  // get isArray(): boolean {
  //   return this.result != null &&
  //     this.result.constructor === Array;
  // }
}


import {Component, Input, OnInit} from '@angular/core';
import {Field, Schema, Type, TypedInstance} from '../../services/schema';
import {TypeNamedInstance} from '../../services/query.service';
import {InstanceLikeOrCollection} from '../../object-view/object-view.component';

@Component({
  selector: 'result-viewer',
  templateUrl: './result-viewer.component.html',
  styleUrls: ['./result-viewer.component.scss']
})
export class ResultViewerComponent implements OnInit {

  @Input()
    // result: TypeInstanceOrAttributeSet;
  result: InstanceLikeOrCollection;

  @Input()
  schema: Schema;

  ngOnInit() {
  }

  get typedObject(): TypeNamedInstance {
    return <TypeNamedInstance>this.result;
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
    return this.schema.types.find(type => type.name.parameterizedName === typeRef.type.parameterizedName);
  }


  // Indicates if it's a straight typedInstance (ie., a typedValue)
  // or a typed object, which is indexed with property names

  get isPrimitive(): boolean {
    return this.result != null && this.typedObject.value != null && !this.isTypedObject && !this.isArray;
  }

  get isTypedObject(): boolean {
    return this.result != null &&
      !this.isArray &&
      typeof this.typedObject.value === 'object';
    // this.result.hasOwnProperty("type")
    // && (<any>this.result).type.hasOwnProperty("fullyQualifiedName")
  }

  get isArray(): boolean {
    return this.result != null &&
      this.result.constructor === Array;
  }
}

type TypeInstanceOrAttributeSet = TypedInstance | TypedObjectAttributes;

interface TypedObjectAttributes {
  [key: string]: TypeInstanceOrAttributeSet;
}


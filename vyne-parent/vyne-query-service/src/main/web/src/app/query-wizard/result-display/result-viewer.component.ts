import {Component, Input, OnInit} from '@angular/core';
import {Schema, Type, TypedInstance, TypeReference} from "../../services/schema";

@Component({
  selector: 'result-viewer',
  templateUrl: './result-viewer.component.html',
  styleUrls: ['./result-viewer.component.scss']
})
export class ResultViewerComponent implements OnInit {

  @Input()
  result: TypeInstanceOrAttributeSet;

  @Input()
  schema: Schema;

  @Input()
  type: Type;

  ngOnInit() {
  }

  get typedObject(): TypedObjectAttributes {
    return <TypedObjectAttributes>this.result;
  }

  get typedObjectAttributeNames(): string[] {
    // return Array.from(this.type.attributes.keys())
    return Object.keys(this.type.attributes)
  }

  getTypedObjectAttribute(name: string): TypeInstanceOrAttributeSet {
    return this.typedObject[name]
  }

  getTypeForAttribute(attributeName: string): Type {
    let typeRef: TypeReference = this.type.attributes[attributeName];
    return this.schema.types.find(type => type.name.fullyQualifiedName == typeRef.fullyQualifiedName)
  }


  // Indicates if it's a straight typedInstance (ie., a typedValue)
  // or a typed object, which is indexed with property names

  get isPrimitive():boolean {
    return !this.isTypedObject && !this.isArray;
  }
  get isTypedObject(): boolean {
    return this.result != null &&
      !this.isArray &&
      typeof this.result === "object";
    // this.result.hasOwnProperty("type")
    // && (<any>this.result).type.hasOwnProperty("fullyQualifiedName")
  }

  get isArray(): boolean {
    return this.result != null &&
      this.result.constructor === Array
  }
}

type TypeInstanceOrAttributeSet = TypedInstance | TypedObjectAttributes
type TypedObjectAttributes = { [key: string]: TypeInstanceOrAttributeSet }

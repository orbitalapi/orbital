import {Component, Input, OnInit} from '@angular/core';
import {TypedInstance} from "../../services/types.service";

@Component({
  selector: 'result-viewer',
  templateUrl: './result-viewer.component.html',
  styleUrls: ['./result-viewer.component.scss']
})
export class ResultViewerComponent implements OnInit {

  @Input()
  result: TypeInstanceOrAttributeSet;

  ngOnInit() {
  }

  get typedObject(): TypedObjectAttributes {
    return <TypedObjectAttributes>this.result;
  }

  get typedObjectAttributeNames(): string[] {
    return Object.keys(this.typedObject)
  }

  getTypedObjectAttribute(name: string): TypeInstanceOrAttributeSet {
    return this.typedObject[name]
  }

  get typedValue(): TypedInstance {
    return this.isTypedValue ? (<TypedInstance>this.result) : null;
  }

  // Indicates if it's a straight typedInstance (ie., a typedValue)
  // or a typed object, which is indexed with property names

  get isTypedValue(): boolean {
    return this.result != null &&
      this.result.hasOwnProperty("type")
      && (<any>this.result).type.hasOwnProperty("fullyQualifiedName")

  }
}

type TypeInstanceOrAttributeSet = TypedInstance | TypedObjectAttributes
type TypedObjectAttributes = { [key: string]: TypeInstanceOrAttributeSet }

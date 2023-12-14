import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema, Type} from '../../services/schema';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-workbook-schema-selector',
  templateUrl: './schema-selector.component.html',
  styleUrls: ['./schema-selector.component.scss']
})
export class SchemaSelectorComponent {

  parsingTypeSelectorMode: 'useExisting' | 'createNew' = 'useExisting';
  projectingTypeSelectorMode: 'useExisting' | 'createNew' = 'useExisting';

  projectToAnotherType = false;

  @Input()
  source: string;

  @Input()
  schema: Schema;

  @Input()
  working = false;

  @Input()
  errorMessage: string = null;

  existingParseType: Type;
  customParseType: Type;

  existingProjectionType: Type;
  customProjectionType: Type;

  createdSchema: string;

  private _typesInSchema: Type[];
  @Input()
  get typesInSchema(): Type[] {
    return this._typesInSchema;
  }

  set typesInSchema(types: Type[]) {
    if (this._typesInSchema === types) {
      return;
    }
    this._typesInSchema = types;
    if (isNullOrUndefined(types)) {
      return;
    }
    if (this.customParseType) {
      // Check the selected type is still present in the schema
      // This resets the type if the type definition has changed (but the name has stayed the same)
      this.customParseType = types.find(type => type.name.parameterizedName === this.customParseType.name.parameterizedName);
    }
    if (this.customProjectionType) {
      this.customProjectionType = types.find(type => type.name.parameterizedName === this.customProjectionType.name.parameterizedName);
    }
    if (types.length > 0 && !this.customParseType) {
      this.customParseType = types[0];
    }
  }


  @Output()
  schemaChange = new EventEmitter<string>();

  @Output()
  runClick = new EventEmitter<ParseTypeSelectedEvent>();

  onContentChanged($event: string) {
    if (isNullOrUndefined($event) || $event.length === 0) {
      this.customParseType = null;
    }
    this.schemaChange.emit($event);
    this.createdSchema = $event;
  }

  compareTypes(a: Type, b: Type): boolean {
    return a && b && a.name.parameterizedName === b.name.parameterizedName;
  }

  onCustomerParseTypeSelected(type: Type) {
    this.customParseType = type;
  }

  get hasType(): boolean {
    if (this.parsingTypeSelectorMode === 'createNew') {
      return (this.typesInSchema && this.typesInSchema.length === 1 || this.typesInSchema && !isNullOrUndefined(this.customParseType));
    } else {
      return !isNullOrUndefined(this.existingParseType);
    }
  }

  onRunClicked() {
    const customSchema = (this.parsingTypeSelectorMode === 'createNew' || this.projectingTypeSelectorMode === 'createNew')
      ? this.createdSchema : null;
    const parseType = (this.parsingTypeSelectorMode === 'createNew') ? this.customParseType : this.existingParseType;
    let projectionType: Type = null;
    if (this.projectToAnotherType) {
      projectionType = this.projectingTypeSelectorMode === 'createNew' ? this.customProjectionType : this.existingProjectionType;
    }
    this.runClick.emit(new ParseTypeSelectedEvent(
      customSchema,
      parseType,
      projectionType
    ));
  }
}

export class ParseTypeSelectedEvent {
  constructor(public readonly schema: string | null, public readonly parseType: Type, public readonly projectionType: Type | null) {
  }
}

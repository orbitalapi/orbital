import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema, Type} from '../../services/schema';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-workbook-schema-selector',
  templateUrl: './schema-selector.component.html',
  styleUrls: ['./schema-selector.component.scss']
})
export class SchemaSelectorComponent {

  parsingTypeSelectorMode: 'useExisting' | 'createNew' = 'createNew';
  projectingTypeSelectorMode: 'useExisting' | 'createNew' = 'createNew';

  projectToAnotherType = false;

  @Input()
  title: string;

  @Input()
  source: string;

  @Input()
  schema: Schema;

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

  set typesInSchema(value: Type[]) {
    if (this._typesInSchema === value) {
      return;
    }
    this._typesInSchema = value;
    if (isNullOrUndefined(value)) {
      return;
    }
    if (this.customParseType) {
      // Check the selected type is still present in the schema
      if (!value.find(type => type.name.parameterizedName === this.customParseType.name.parameterizedName)) {
        this.customParseType = null;
      }
    }
    if (value.length > 0 && !this.customParseType) {
      this.customParseType = value[0];
    }
  }


  @Output()
  schemaChange = new EventEmitter<string>();

  @Output()
  targetTypeSelected = new EventEmitter<ParseTypeSelectedEvent>();

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
    this.targetTypeSelected.emit(new ParseTypeSelectedEvent(
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

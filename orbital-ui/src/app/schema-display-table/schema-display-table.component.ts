import {Component, Input, OnInit} from '@angular/core';
import {Field, findType, QualifiedName, Schema, Type} from '../services/schema';

@Component({
  selector: 'app-schema-display-table',
  template: `
    <div class="type-container">
      <div class="title">{{ type.name.shortDisplayName }}</div>
      <table class="field-list">
        <tr class="field-row" *ngFor="let field of fields | keyvalue">
          <td *ngIf="hasObjectTypes" class="button-column">
            <button class="expand-button" *ngIf="!field.value.type.isScalar"
                    (click)="field.value.expanded = !field.value.expanded">
              {{ field.value.expanded ? '-' : '+' }}
            </button>
          </td>
          <td>{{field.key}}</td>
          <td class="type-name" *ngIf="!field.value.expanded">
            <span>{{field.value.name.shortDisplayName}}</span>
            <span class="primitive-type-name"
                  *ngIf="field.value.type.isScalar">({{field.value.type.basePrimitiveTypeName.shortDisplayName}})</span>
          </td>
          <td *ngIf="field.value.expanded">
            <app-schema-display-table [schema]="schema" [type]="field.value.type"></app-schema-display-table>
          </td>
        </tr>
      </table>
    </div>
  `,
  styleUrls: ['./schema-display-table.component.scss']
})
export class SchemaDisplayTableComponent implements OnInit {

  private _type: Type;

  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    if (this._type === value) {
      return;
    }
    this._type = value;
    this.populateFields();
  }

  private _schema: Schema;

  @Input()
  get schema(): Schema {
    return this._schema;
  }

  set schema(value: Schema) {
    if (this._schema === value) {
      return;
    }
    this._schema = value;
    this.populateFields();
  }

  // The actual type to iterate.  If the type is a collection, then this is
  // the collection member type, otherwise the same value as type.
  private underlyingType: Type;

  fields: {
    [key: string]: {
      field: Field,
      type: Type,
      name: QualifiedName,
      expanded: boolean;
    }
  };

  hasObjectTypes: boolean;

  private populateFields() {
    if (!this.type || !this.schema) {
      return;
    }
    this.underlyingType = this.type.collectionType || this.type;
    this.fields = {};
    this.hasObjectTypes = false;
    Object.keys(this.underlyingType.attributes).forEach(fieldName => {
      const field = this.underlyingType.attributes[fieldName];
      const type = findType(this.schema, field.type.parameterizedName);
      if (!type.isScalar) {
        this.hasObjectTypes = true;
      }
      this.fields[fieldName] = {
        field,
        name: type.name,
        type: type.collectionType || type,
        expanded: false
      };
    });
  }

  ngOnInit(): void {

  }
}

import {Component, Input, OnInit} from '@angular/core';
import {
  Field,
  isOperation,
  isService,
  isType,
  Operation,
  SchemaMember,
  Service,
  Type,
  TypeReference
} from '../../services/schema';
import {error} from '@angular/compiler/src/util';

@Component({
  selector: 'app-attribute-table',
  templateUrl: './attribute-table.component.html',
  styleUrls: ['./attribute-table.component.scss']
})
export class AttributeTableComponent {
  schemaMember: SchemaMember;
  private _type: Type;
  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    this._type = value;
    if (isType(value)) {
      this.schemaMember = SchemaMember.fromType(value);
    } else {
      error('value is not a Type');
    }
  }

  get hasAttributes(): boolean {
    return this.attributeNames.length > 0;
  }

  get attributeNames(): string[] {
    if (!this.schemaMember) {
      return [];
    }
    return this.schemaMember.attributeNames;
  }


  attributeType(name: string): TypeReference {
    const field: Field = this.type.attributes[name];
    return field.type;
  }


  routerLinkFor(attributeName: string): string[] {
    return ['/types', this.attributeType(attributeName).fullyQualifiedName];
  }
}

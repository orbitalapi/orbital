import {Component, Input} from '@angular/core';
import {EnumValues, isType, QualifiedName, SchemaMember, Type} from '../../services/schema';
import {error} from '@angular/compiler/src/util';

@Component({
  selector: 'app-enum-table',
  templateUrl: './enum-table.component.html',
  styleUrls: ['./enum-table.component.scss']
})
export class EnumTableComponent {
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

  get enums(): EnumValues[] {
    if (!this.schemaMember) {
      return [];
    }
    return this.type.enumValues;
  }

  typeDoc(index: number): string {
    return this.type.enumValues[index].typeDoc || "";
  }

  enumValues(): EnumValues[] {
    return this.type.enumValues;
  }

  shortDisplayName(i: number, index: number): string {
    const synonym = this.type.enumValues[i].synonyms[index] || "";
    return synonym || "";
  }

  longDisplayName(): string {
    return this.type.name.longDisplayName;
  }

  synonyms(index?: number): EnumValues {
    return this.type.enumValues[index]
  }

  routerLinkFor(i: number, index: number): string[] {
    const synonym = this.type.enumValues[i].synonyms[index];
    let route;
    if (synonym) {
      route = QualifiedName.from(synonym).namespace;
    }
    return ['/types', route || ""];
  }
}

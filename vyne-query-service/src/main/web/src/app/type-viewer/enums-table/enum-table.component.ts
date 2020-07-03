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


  typeDoc(index: number): string {
    return this.type.enumValues[index].typeDoc || '';
  }

  routerLinkFor(synonym: string): string[] {
    const parts = synonym.split('.');
    const enumName = parts.slice(0, parts.length - 1).join('.');
    return ['/types', enumName];
  }
}

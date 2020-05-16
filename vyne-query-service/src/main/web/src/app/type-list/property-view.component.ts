import {Component, Input, OnInit} from '@angular/core';
import {Field, QualifiedName, SchemaMember, Type} from '../services/schema';

@Component({
  selector: 'type-property-view',
  templateUrl: './property-view.component.html',
  styleUrls: ['./property-view.component.scss']
})
export class PropertyViewComponent implements OnInit {

  constructor() {
  }

  ngOnInit() {
  }

  @Input()
  schemaMember: SchemaMember;

  get hasAttributes(): boolean {
    return this.attributeNames.length > 0;
  }

  get attributeNames(): string[] {
    if (!this.schemaMember) { return []; }
    return this.schemaMember.attributeNames;
  }

  displayedColumns: string[] = ['name1', 'type'];

  // displayedColumns: string[] = ['position', 'name', 'weight', 'symbol'];

  private get type(): Type {
    return <Type>this.schemaMember.member;
  }

  attribute(name: string): QualifiedName {
    const field: Field = this.type.attributes[name];
    return field.type;
  }

}

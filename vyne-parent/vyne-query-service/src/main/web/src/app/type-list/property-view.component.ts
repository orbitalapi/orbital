import {Component, Input, OnInit} from '@angular/core';
import {SchemaMember, Type, TypeReference} from "../services/types.service";

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

  ELEMENT_DATA: any[] = [
    {position: 1, name: 'Hydrogen', weight: 1.0079, symbol: 'H'},
    {position: 2, name: 'Helium', weight: 4.0026, symbol: 'He'},
    {position: 3, name: 'Lithium', weight: 6.941, symbol: 'Li'},
    {position: 4, name: 'Beryllium', weight: 9.0122, symbol: 'Be'},
    {position: 5, name: 'Boron', weight: 10.811, symbol: 'B'},
    {position: 6, name: 'Carbon', weight: 12.0107, symbol: 'C'},
    {position: 7, name: 'Nitrogen', weight: 14.0067, symbol: 'N'},
    {position: 8, name: 'Oxygen', weight: 15.9994, symbol: 'O'},
    {position: 9, name: 'Fluorine', weight: 18.9984, symbol: 'F'},
    {position: 10, name: 'Neon', weight: 20.1797, symbol: 'Ne'},
  ];

  @Input()
  schemaMember: SchemaMember;

  get hasAttributes():boolean {
    return this.attributeNames.length > 0;
  }

  get attributeNames(): string[] {
    if (!this.schemaMember) return [];
    return this.schemaMember.attributeNames
  }

  displayedColumns: string[] = ["name1", "type"];
  // displayedColumns: string[] = ['position', 'name', 'weight', 'symbol'];

  private get type(): Type {
    return <Type>this.schemaMember.member;
  }

  attribute(name: string): TypeReference {
    return this.type.attributes[name]
  }

}

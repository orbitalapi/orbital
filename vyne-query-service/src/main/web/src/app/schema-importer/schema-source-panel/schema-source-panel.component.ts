import {Component, OnInit, ViewChild} from '@angular/core';

@Component({
  selector: 'app-schema-source-panel',
  styleUrls: ['./schema-source-panel.component.scss'],
  template: `
    <div>
      <tui-combo-box
        tuiTextfieldExampleText="Type of schema"
        [stringify]="stringify"
        [(ngModel)]="schemaType"
      >
        Select a schema type to import
        <tui-data-list-wrapper
          *tuiDataList
          [items]="schemaTypes | tuiFilterByInputWith : stringify"
          [itemContent]="stringify | tuiStringifyContent"
        ></tui-data-list-wrapper>
      </tui-combo-box>
      <div [ngSwitch]="schemaType?.id">
        <app-swagger-config *ngSwitchCase="'swagger'"></app-swagger-config>
        <app-jsonschema-config *ngSwitchCase="'jsonschema'"></app-jsonschema-config>
        <app-database-table-config *ngSwitchCase="'databaseTable'"></app-database-table-config>
      </div>
    </div>
  `
})
export class SchemaSourcePanelComponent  {

  schemaTypes: SchemaType[] = [
    // { 'label' : 'Taxi', id: 'taxi'},
    { 'label' : 'Swagger / OpenAPI', id: 'swagger'},
    { 'label' : 'JsonSchema', id: 'jsonschema'},
    { 'label' : 'Database table', id: 'databaseTable'},
    // { 'label' : 'XML Schema (xsd)', id: 'xsd'},
  ]

  schemaType: SchemaType

  readonly stringify = (item:SchemaType) => item.label;


}


export interface SchemaType {
  label: string;
  id: string;
}

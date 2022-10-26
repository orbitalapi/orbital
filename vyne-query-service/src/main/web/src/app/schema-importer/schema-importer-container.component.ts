import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-schema-importer-container',
  template: `
    <app-header-bar title="Schema Importer">
    </app-header-bar>
    <app-schema-importer></app-schema-importer>
  `,
  styleUrls: ['./schema-importer-container.component.scss']
})
export class SchemaImporterContainerComponent {}

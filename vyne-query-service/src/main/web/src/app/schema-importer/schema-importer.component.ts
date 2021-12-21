import {Component, Input, OnInit} from '@angular/core';
import {SchemaGenerationResult} from '../services/types.service';
import {Operation, SchemaMember, Service, ServiceMember, Type} from '../services/schema';


@Component({
  selector: 'app-schema-importer',
  templateUrl: './schema-importer.component.html',
  styleUrls: ['./schema-importer.component.scss']
})
export class SchemaImporterComponent {

  @Input()
  importedSchema: SchemaGenerationResult;
}

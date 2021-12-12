import {Component, Input, OnInit} from '@angular/core';
import {TaxiSubmissionResult} from '../services/types.service';
import {Operation, SchemaMember, Service, ServiceMember, Type} from '../services/schema';

export interface AccordionEntry {
  label: string;
  member: Type | Service | Operation
  category: 'type' | 'service' | 'operation' | 'model';
}

export interface ServiceAccordionEntry extends AccordionEntry {
  member: Service
  category: 'service';
  operations: ServiceMember[];
}


@Component({
  selector: 'app-schema-importer',
  templateUrl: './schema-importer.component.html',
  styleUrls: ['./schema-importer.component.scss']
})
export class SchemaImporterComponent {
  private _importedSchema: TaxiSubmissionResult;

  types: AccordionEntry[];
  models: AccordionEntry[];
  services: ServiceAccordionEntry[];

  @Input()
  get importedSchema(): TaxiSubmissionResult {
    return this._importedSchema;
  }

  set importedSchema(value: TaxiSubmissionResult) {
    if (this.importedSchema === value) {
      return;
    }
    this._importedSchema = value;
    if (value) {
      const [types, models, services] = this.buildTree(this.importedSchema);
      this.types = types;
      this.models = models;
      this.services = services;
    }
  }


  private buildTree(schema: TaxiSubmissionResult): [AccordionEntry[], AccordionEntry[], ServiceAccordionEntry[]] {
    function typeToEntry(type: Type): AccordionEntry {
      return {
        category: 'type',
        label: type.name.shortDisplayName,
        member: type
      }
    }

    const types = schema
      .types
      .filter(type => type.isScalar)
      .map(type => typeToEntry(type))
    const models = schema
      .types
      .filter(type => !type.isScalar)
      .map(type => typeToEntry(type))

    const services = schema
      .services
      .map(service => {
        return {
          category: 'service',
          label: service.name.shortDisplayName,
          member: service,
          operations: (service.operations as ServiceMember[]).concat(service.queryOperations)
        } as ServiceAccordionEntry
      })

    return [types, models, services]
  }
}

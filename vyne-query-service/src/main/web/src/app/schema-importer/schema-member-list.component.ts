import {Component, Input, OnInit} from '@angular/core';
import {SchemaGenerationResult} from '../services/types.service';
import {Operation, Service, ServiceMember, Type} from '../services/schema';

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
  selector: 'app-schema-member-list',
  template: `
    <tui-accordion>
      <tui-accordion-item [showArrow]="true">Models <tui-badge
        size="s"
        status="info"
        [value]="models.length"
      ></tui-badge>
        <ng-template tuiAccordionItemContent [tuiTreeController]="true">
          <tui-tree-item *ngFor="let entry of models">{{ entry.label }}</tui-tree-item>
        </ng-template>
      </tui-accordion-item>
      <tui-accordion-item [showArrow]="true">Types
        <tui-badge
          size="s"
          status="info"
          [value]="types.length"
        ></tui-badge>
        <ng-template tuiAccordionItemContent [tuiTreeController]="true">
          <tui-tree-item *ngFor="let entry of types">{{ entry.label }}</tui-tree-item>
        </ng-template>
      </tui-accordion-item>
      <tui-accordion-item [showArrow]="true">Services
        <tui-badge
          size="s"
          status="info"
          [value]="services.length"
        ></tui-badge>
        <ng-template tuiAccordionItemContent [tuiTreeController]="true">
          <tui-tree-item *ngFor="let service of services">{{ service.label }}
            <tui-tree-item *ngFor="let operation of service.operations">{{ operation.qualifiedName.shortDisplayName }}</tui-tree-item>
          </tui-tree-item>
        </ng-template>
      </tui-accordion-item>
    </tui-accordion>

  `,
  styleUrls: ['./schema-member-list.component.scss']
})
export class SchemaMemberListComponent  {

  private _importedSchema: SchemaGenerationResult;

  types: AccordionEntry[];
  models: AccordionEntry[];
  services: ServiceAccordionEntry[];

  @Input()
  get importedSchema(): SchemaGenerationResult {
    return this._importedSchema;
  }

  set importedSchema(value: SchemaGenerationResult) {
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


  private buildTree(schema: SchemaGenerationResult): [AccordionEntry[], AccordionEntry[], ServiceAccordionEntry[]] {
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

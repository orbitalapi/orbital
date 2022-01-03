import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {SchemaSubmissionResult} from '../../services/types.service';
import {Operation, QueryOperation, Service, ServiceMember, Type} from '../../services/schema';

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
  selector: 'app-schema-entry-table',
  template: `
    <tui-accordion>
      <tui-accordion-item [showArrow]="true">Models
        <tui-badge
          size="s"
          status="info"
          [value]="models.length"
        ></tui-badge>
        <ng-template tuiAccordionItemContent [tuiTreeController]="true">
          <tui-tree-item *ngFor="let entry of models">
            <button tuiButton [appearance]="'flat'" [size]="'m'"
                    (click)="onModelSelected(entry)">{{ entry.label }}</button>
          </tui-tree-item>
        </ng-template>
      </tui-accordion-item>
      <tui-accordion-item [showArrow]="true">Types
        <tui-badge
          size="s"
          status="info"
          [value]="types.length"
        ></tui-badge>
        <ng-template tuiAccordionItemContent [tuiTreeController]="true">
          <tui-tree-item *ngFor="let entry of types">
            <button tuiButton [appearance]="'flat'" [size]="'m'"
                    (click)="onModelSelected(entry)">{{ entry.label }}</button>
          </tui-tree-item>
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
            <tui-tree-item
              *ngFor="let operation of service.operations">

              <button tuiButton [appearance]="'flat'" [size]="'m'"
                      (click)="onOperationSelected(operation)">{{ operation.qualifiedName.shortDisplayName }}</button>
            </tui-tree-item>
          </tui-tree-item>
        </ng-template>
      </tui-accordion-item>
    </tui-accordion>

  `,
  styleUrls: ['./schema-entry-table.component.scss']
})
export class SchemaEntryTableComponent {

  private _importedSchema: SchemaSubmissionResult;

  types: AccordionEntry[];
  models: AccordionEntry[];
  services: ServiceAccordionEntry[];

  @Output()
  modelSelected = new EventEmitter<Type>()

  @Output()
  operationSelected = new EventEmitter<ServiceMember>()

  @Input()
  get importedSchema(): SchemaSubmissionResult {
    return this._importedSchema;
  }

  set importedSchema(value: SchemaSubmissionResult) {
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


  private buildTree(schema: SchemaSubmissionResult): [AccordionEntry[], AccordionEntry[], ServiceAccordionEntry[]] {
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

  onModelSelected(entry: AccordionEntry) {
    this.modelSelected.emit(entry.member as Type);
  }

  onOperationSelected(operation: ServiceMember) {
    this.operationSelected.emit(operation)
  }
}

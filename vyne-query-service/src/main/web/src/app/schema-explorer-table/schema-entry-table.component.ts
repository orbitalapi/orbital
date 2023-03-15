import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { Operation, PartialSchema, Service, ServiceMember, Type } from 'src/app/services/schema';
import { Observable } from 'rxjs/internal/Observable';

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
    <tui-accordion *ngIf="schemaReceived">
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
              *ngFor="let operation of collectOperations(service.member)">

              <button tuiButton [appearance]="'flat'" [size]="'m'"
                      (click)="onOperationSelected(operation)">{{ operation.qualifiedName.shortDisplayName }}</button>
            </tui-tree-item>
          </tui-tree-item>
        </ng-template>
      </tui-accordion-item>
    </tui-accordion>

  `,
  styleUrls: ['./schema-entry-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SchemaEntryTableComponent {

  constructor(private changeDetector: ChangeDetectorRef) {
  }

  collectOperations(service: Service): ServiceMember[] {
    return [...service.operations, ...service.queryOperations, ...service.tableOperations, ...service.streamOperations];
  }

  private _importedSchema: Observable<PartialSchema>;

  types: AccordionEntry[];
  models: AccordionEntry[];
  services: ServiceAccordionEntry[];

  schemaReceived = false;
  @Output()
  modelSelected = new EventEmitter<Type>()

  @Output()
  operationSelected = new EventEmitter<ServiceMember>()

  @Input()
  get partialSchema$(): Observable<PartialSchema> {
    return this._importedSchema;
  }

  set partialSchema$(value) {
    if (this.partialSchema$ === value) {
      return;
    }
    this._importedSchema = value;
    this.schemaReceived = false;
    this.partialSchema$.subscribe(next => {
      this.schemaReceived = true;
      const [types, models, services] = this.buildTree(next);
      this.types = types;
      this.models = models;
      this.services = services;
      this.changeDetector.markForCheck();
    })
  }


  private buildTree(schema: PartialSchema): [AccordionEntry[], AccordionEntry[], ServiceAccordionEntry[]] {
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
      .sort((a, b) => a.label.localeCompare(b.label));
    const models = schema
      .types
      .filter(type => !type.isScalar)
      .map(type => typeToEntry(type))
      .sort((a, b) => a.label.localeCompare(b.label));
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
      .sort((a, b) => a.label.localeCompare(b.label));

    return [types, models, services]
  }

  onModelSelected(entry: AccordionEntry) {
    this.modelSelected.emit(entry.member as Type);
  }

  onOperationSelected(operation: ServiceMember) {
    this.operationSelected.emit(operation)
  }
}

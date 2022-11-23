import { ChangeDetectionStrategy, Component, Input, ViewChild } from '@angular/core';
import { findSchemaMember, Schema, Service } from 'src/app/services/schema';
import { Observable } from 'rxjs/internal/Observable';
import { SchemaDiagramComponent } from 'src/app/schema-diagram/schema-diagram/schema-diagram.component';

@Component({
  selector: 'app-lineage-node-diagram',
  styleUrls: ['./lineage-node-diagram.component.scss'],
  template: `
    <app-schema-diagram #diagramComponent [title]="title" [schema$]="schema$" [displayedMembers]="displayedServices"></app-schema-diagram>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LineageNodeDiagramComponent {
  private _initialServices: string[]
  @Input()
  get initialServices(): string[] {
    return this._initialServices;
  }

  set initialServices(value: string[]) {
    this._initialServices = value;
    this.resetComponent();
  }

  private _schema$: Observable<Schema>;

  @ViewChild('diagramComponent')
  private diagramComponent: SchemaDiagramComponent;

  displayedServices: string[]

  @Input()
  get schema$(): Observable<Schema> {
    return this._schema$;
  }

  set schema$(value: Observable<Schema>) {
    this._schema$ = value;
    this.resetComponent();
  }

  @Input()
  title: string;

  @Input()
  requiredServices: string[]


  resetComponent() {
    if (!this.initialServices || !this.schema$) {
      return;
    }
    if (this.diagramComponent) {
      this.diagramComponent.resetComponent();
    }
    this.schema$.subscribe(schema => {
      const servicesWithDependencies = this.initialServices.flatMap(serviceName => {
        const member = findSchemaMember(schema, serviceName)
        const service = member.member as Service;
        const dependentServices = new Set<string>();
        const consumedBy: string[] = schema.services.flatMap(service => {
          const serviceConsumedOperations = service.lineage?.consumes || [];
          if (serviceConsumedOperations.some(c => c.serviceName === serviceName)) {
            return [service.qualifiedName]
          } else {
            return []
          }
        });
        consumedBy.forEach(s => dependentServices.add(s));

        const consumedOperations = service.lineage?.consumes || [];
        consumedOperations.map(consumedOperation => {
          dependentServices.add(consumedOperation.serviceName)
        })
        return Array.from(dependentServices)
      }).concat(this._initialServices)

      this.displayedServices = servicesWithDependencies;
    })
  }
}


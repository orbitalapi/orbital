import { ChangeDetectionStrategy, Component, Input, ViewChild } from '@angular/core';
import { TypesService } from '../../services/types.service';
import {
  arrayMemberTypeNameOrTypeNameFromName,
  findSchemaMember,
  QualifiedName,
  Schema,
  SchemaGraph,
  Type
} from '../../services/schema';
import { Observable } from 'rxjs/internal/Observable';
import { tap } from 'rxjs/operators';
import {
  buildLinksForType,
  collectionOperations,
  collectLinks
} from 'src/app/schema-diagram/schema-diagram/schema-chart-builder';
import {
  LineageNodeDiagramComponent
} from 'src/app/type-viewer/lineage-graph/lineage-node-diagram/lineage-node-diagram.component';

@Component({
  selector: 'app-service-lineage-graph-container',
  template: `
    <app-lineage-node-diagram #diagramComponent [schema$]="schema$" [initialServices]="displayedMembers"></app-lineage-node-diagram>
  `,
  styleUrls: ['./service-lineage-graph-container.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServiceLineageGraphContainerComponent {
  private _serviceName: QualifiedName;
  private _type: Type;

  schema$: Observable<Schema>
  private schema: Schema;

  displayedMembers: string[] = [];

  constructor(private typeService: TypesService) {
    this.schema$ = typeService.getTypes()
      .pipe(
        tap(schema => {
          this.schema = schema;
          this.buildDependenciesForType();
        })
      )
  }

  @ViewChild('diagramComponent')
  diagramComponent: LineageNodeDiagramComponent

  @Input()
  get serviceName(): QualifiedName {
    return this._serviceName;
  }

  set serviceName(value: QualifiedName) {
    if (this.serviceName === value) {
      return;
    }
    this.displayedMembers = [value.fullyQualifiedName]
    this._serviceName = value;
  }


  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    if (this.type === value) {
      return;
    }
    this._type = value;
    if (this.diagramComponent) {
      this.diagramComponent.resetComponent();
    }
    this.buildDependenciesForType();
  }

  private buildDependenciesForType() {
    if (!this.schema || !this.type) {
      return;
    }

    // Update displayed members to show all services that relate to this type
    const operations = collectionOperations(this.schema);
    const links = buildLinksForType(this.type.name, this.schema, operations, null)
    const allRelatedNames = collectLinks(links).flatMap(link => {
      return [
        arrayMemberTypeNameOrTypeNameFromName(link.sourceNodeName),
        arrayMemberTypeNameOrTypeNameFromName(link.targetNodeName)
      ]
    }).filter(name => {
      const member = findSchemaMember(this.schema, name.fullyQualifiedName)
      return member.kind === 'SERVICE' || member.kind === 'OPERATION'
    })
    const uniqueRelatedNames = Array.from(new Set(allRelatedNames));
    this.displayedMembers = [this.type.name.fullyQualifiedName].concat(uniqueRelatedNames.map(name => name.fullyQualifiedName));
  }


}

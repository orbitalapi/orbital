import {Component, Input, OnInit} from '@angular/core';
import {TypesService} from '../../services/types.service';
import {QualifiedName, SchemaGraph, SchemaNodeSet, Type} from '../../services/schema';
import {Observable} from 'rxjs/internal/Observable';

@Component({
  selector: 'app-service-lineage-graph-container',
  template: `
    <div style="width: 100%; height: 100%;">
      <app-service-lineage-graph [schemaGraph$]="schemaGraph"></app-service-lineage-graph>
    </div>
  `,
  styleUrls: ['./service-lineage-graph-container.component.scss']
})
export class ServiceLineageGraphContainerComponent {
  private _serviceName: QualifiedName;
  private _type: Type;

  constructor(private typeService: TypesService) {
  }

  @Input()
  get serviceName(): QualifiedName {
    return this._serviceName;
  }

  set serviceName(value: QualifiedName) {
    if (this.serviceName === value) {
      return;
    }
    this._serviceName = value;
    if (this.serviceName) {
      this.schemaGraph = this.typeService.getServiceLineage(this.serviceName.fullyQualifiedName);
    }
  }
  schemaGraph: Observable<SchemaGraph>;


  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    if (this.type === value) {
      return;
    }
    this._type = value;
    if (this.type) {
      this.schemaGraph = this.typeService.getTypeLineage(this.type.name.parameterizedName);
    }
  }





}

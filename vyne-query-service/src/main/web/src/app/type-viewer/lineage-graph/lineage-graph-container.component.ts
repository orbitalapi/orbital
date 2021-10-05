import {Component, Input, OnInit} from '@angular/core';
import {TypesService} from '../../services/types.service';
import {SchemaGraph, SchemaNodeSet, Type} from '../../services/schema';
import {Observable} from 'rxjs/internal/Observable';

@Component({
  selector: 'app-lineage-graph-container',
  template: `
    <div style="width: 100%; height: 100%;">
      <app-lineage-graph [schemaGraph$]="schemaGraph"></app-lineage-graph>
    </div>
  `,
  styleUrls: ['./lineage-graph-container.component.scss']
})
export class LineageGraphContainerComponent {
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

  constructor(private typeService: TypesService) {
  }

  private _type: Type;

}

import {Component, Input, OnInit} from '@angular/core';
import {QualifiedName, SchemaGraph, SchemaGraphNode, SchemaMember} from '../../services/schema';
import {TypesService} from '../../services/types.service';
import {Subject} from 'rxjs';


@Component({
  selector: 'app-type-link-graph-container',
  template: `
    <div style="width: 100%; height: 100%;">
        <app-type-link-graph [schemaGraphs]="schemaGraphs" (nodeClicked)="onNodeClicked($event)"></app-type-link-graph>
    </div>
  `
})
export class TypeLinkGraphContainerComponent {
  private _member: SchemaMember = null;

  schemaGraphs: Subject<SchemaGraph> = new Subject<SchemaGraph>();

  @Input()
  set member(value: SchemaMember) {
    this._member = value;
    if (this._member) {
      this.loadTypeLinks(this._member.name);
    }
  }

  get member(): SchemaMember {
    return this._member;
  }

  constructor(private service: TypesService) {
  }

  private loadTypeLinks(name: QualifiedName) {
    const sanitized = name.fullyQualifiedName.replace(' #', '@@');
    this.service.getLinks(sanitized).subscribe(value => {
      this.schemaGraphs.next(value);
    });
  }

  onNodeClicked(node: SchemaGraphNode) {
    this.service.getLinksForNode(node).subscribe(value => this.schemaGraphs.next(value));
  }
}

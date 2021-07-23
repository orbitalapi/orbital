import {Component, EventEmitter, Input, Output} from '@angular/core';
import {DataSource, InstanceLike, QualifiedName, Type} from '../services/schema';
import {TypesService} from '../services/types.service';
import {buildInheritable, Inheritable} from '../inheritence-graph/inheritance-graph.component';
import {QueryResultMemberCoordinates} from '../query-panel/instance-selected-event';

@Component({
  selector: 'app-typed-instance-panel-container',
  template: `
      <app-typed-instance-panel
        (hasTypedInstanceDrawerClosed)="onCloseTypedInstanceDrawer($event)"
        [type]="type"
        [instance]="instance"
        [inheritanceView]="inheritanceView"
        [dataSource]="dataSource"
        [discoverableTypes]="discoverableTypes"
        [instanceQueryCoordinates]="instanceQueryCoordinates"
      ></app-typed-instance-panel>
   `
})
export class TypedInstancePanelContainerComponent {

  private _type: Type;

  @Input()
  instance: InstanceLike;

  @Input()
  dataSource: DataSource;

  inheritanceView: Inheritable;

  discoverableTypes: QualifiedName[];

  @Input()
  instanceQueryCoordinates: QueryResultMemberCoordinates;

  @Output() hasTypedInstanceDrawerClosed = new EventEmitter<boolean>();


  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    this._type = value;
    if (this.type) {
      this.typeService.getDiscoverableTypes(this.type.name.fullyQualifiedName)
        .subscribe(result => {
          this.discoverableTypes = result;
        });

      this.typeService.getTypes().subscribe(schema => {
        this.inheritanceView = buildInheritable(this.type, schema);
      });
    }
  }

  constructor(private typeService: TypesService) {
  }

  onCloseTypedInstanceDrawer($event: any) {
    this.hasTypedInstanceDrawerClosed.emit($event);
  }

}

import {Component, Input} from '@angular/core';
import {InstanceLike} from '../object-view/object-view.component';
import {QualifiedName, Type} from '../services/schema';
import {TypesService} from '../services/types.service';
import {buildInheritable, Inheritable} from '../inheritence-graph/inheritance-graph.component';

@Component({
  selector: 'app-typed-instance-panel-container',
  template: `
    <app-typed-instance-panel [type]="type"
                              [instance]="instance"
                              [inheritanceView]="inheritanceView"
                              [discoverableTypes]="discoverableTypes"></app-typed-instance-panel>`
})
export class TypedInstancePanelContainerComponent {

  private _type: Type;

  @Input()
  instance: InstanceLike;

  inheritanceView: Inheritable;

  discoverableTypes: QualifiedName[];

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

}

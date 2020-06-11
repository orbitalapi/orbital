import {Component, Input} from '@angular/core';
import {InstanceLike} from '../object-view/object-view.component';
import {QualifiedName, Type} from '../services/schema';
import {TypesService} from '../services/types.service';

@Component({
  selector: 'app-typed-instance-panel-container',
  template: `
    <app-typed-instance-panel [type]="type"
                              [instance]="instance"
                              [discoverableTypes]="discoverableTypes"></app-typed-instance-panel>`
})
export class TypedInstancePanelContainerComponent {

  private _type: Type;

  @Input()
  instance: InstanceLike;

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
    }
  }

  constructor(private typeService: TypesService) {
  }

}

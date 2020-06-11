import {Component, Input} from '@angular/core';
import {QualifiedName, Type} from '../services/schema';
import {InstanceLike} from '../object-view/object-view.component';

@Component({
  selector: 'app-typed-instance-panel',
  template: `
    <div class="container" *ngIf="type">
      <div class="type-name">
        <h2>{{instance?.value}}</h2>
        <h4>{{type?.name?.name}}</h4>
        <span class="mono-badge">{{type?.name?.fullyQualifiedName}}</span>
      </div>
      <section>
        <app-description-editor-container [type]="type"></app-description-editor-container>
      </section>
      <section *ngIf="hasDiscoverableTypes">
        <h3>Discoverable data</h3>
        <p>Quick link queries to discover relevant data</p>
        <app-inline-query-runner *ngFor="let type of discoverableTypes" [facts]="facts"
                                 [targetType]="type.fullyQualifiedName"></app-inline-query-runner>
      </section>
      <section *ngIf="hasAttributes">
        <h3>Attributes</h3>
        <app-attribute-table [type]="type"></app-attribute-table>
      </section>
      <section *ngIf="hasEnumValues">
        <h3>Possible Values</h3>
        <app-enum-table [type]="type"></app-enum-table>
      </section>
    </div>
  `,
  styleUrls: ['./typed-instance-panel.component.scss']
})
export class TypedInstancePanelComponent {

  @Input()
  instance: InstanceLike;

  @Input()
  type: Type;

  @Input()
  discoverableTypes: QualifiedName[];

  get hasDiscoverableTypes() {
    return this.discoverableTypes && this.discoverableTypes.length > 0;
  }

  get hasAttributes() {
    if (!this.type) {
      return false;
    }
    return this.type.attributes && Object.keys(this.type.attributes).length > 0;
  }

  get hasEnumValues() {
    if (!this.type) {
      return false;
    }
    return this.type.enumValues && Object.keys(this.type.enumValues).length > 0;
  }

  @Input()
  get facts(): InstanceLike[] {
    return [this.instance];
  }

}

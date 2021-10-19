import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {QualifiedName, Type, InstanceLike, DataSource} from '../services/schema';
import {Inheritable} from '../inheritence-graph/inheritance-graph.component';
import {QueryResultMemberCoordinates} from '../query-panel/instance-selected-event';

@Component({
  selector: 'app-typed-instance-panel',
  template: `
    <div class="container" *ngIf="type">
    <span mat-icon-button class="clear-button" (click)=closeTypedInstanceDrawer()>
      <img class="clear-icon" src="assets/img/clear-cross-circle.svg">
    </span>
      <div class="type-name">
        <h2>{{instance?.value}}</h2>
        <h4>{{type?.name?.name}}</h4>
        <span class="mono-badge">{{type?.name?.fullyQualifiedName}}</span>
      </div>
      <section>
        <app-description-editor-container [type]="type"></app-description-editor-container>
      </section>
      <section *ngIf="dataSource">
        <div class="row">
          <h3>Value lineage</h3>
          <button mat-icon-button (click)="openFullLineage()">
            <mat-icon>open_in_full</mat-icon>
          </button>
        </div>

        <app-lineage-display [instance]="instance" [dataSource]="dataSource"
                             [instanceQueryCoordinates]="instanceQueryCoordinates"></app-lineage-display>
      </section>

      <section *ngIf="inheritanceView">
        <h3>Type inheritance</h3>
        <app-inheritance-graph [inheritable]="inheritanceView"></app-inheritance-graph>
      </section>

      <section *ngIf="hasDiscoverableTypes">
        <h3>Discoverable data</h3>
        <p>Quick link queries to discover relevant data</p>
        <app-inline-query-runner *ngFor="let type of discoverableTypes" [facts]="facts"
                                 [targetTypeName]="type"></app-inline-query-runner>
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

  @Input()
  inheritanceView: Inheritable;

  @Input()
  dataSource: DataSource;

  @Output() hasTypedInstanceDrawerClosed = new EventEmitter<boolean>();

  @Input()
  instanceQueryCoordinates: QueryResultMemberCoordinates;

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

  closeTypedInstanceDrawer() {
    this.hasTypedInstanceDrawerClosed.emit(false);
  }

  openFullLineage() {

  }
}

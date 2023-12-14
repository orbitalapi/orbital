import {ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema, SchemaMember, Type} from "../services/schema";

@Component({
  selector: 'app-type-autocomplete-tui',
  styleUrls: ['./type-autocomplete-tui.component.scss'],
  template: `
    <tui-combo-box
      class="type-input"
      [stringify]="stringifyTypeName"
      tuiTextfieldSize="s"
      [valueContent]="value"
      [(ngModel)]="selectedType"
      (ngModelChange)="handleSelectedTypeChanged($event)">
        {{label}}
      <input tuiTextfield [placeholder]="label"/>
      <ng-template #value let-item>
        <div class="type-option">
          <span class="type-name">{{item.name.shortDisplayName}}</span>
          <span class="mono-badge small" *ngIf="item.name.namespace">{{item.name.namespace}}</span>
        </div>
      </ng-template>
      <ng-template tuiDataList>
        <tui-data-list *ngFor="let item of displayTypes | tuiFilterByInputWith : stringifyTypeName">
          <button tuiOption [value]="item">
            <div class="type-option">
              <span class="type-name">{{item.name.shortDisplayName}}</span>
              <span class="mono-badge small" *ngIf="item.name.namespace">{{item.name.namespace}}</span>
            </div>
          </button>
        </tui-data-list>
      </ng-template>
      <!--              <tui-data-list-wrapper-->
      <!--                      *tuiDataList-->
      <!--                      [items]="types | tuiFilterByInputWith : stringifyTypeName"-->
      <!--                      [itemContent]="stringifyTypeName | tuiStringifyContent"></tui-data-list-wrapper>-->
    </tui-combo-box>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TypeAutocompleteTuiComponent {
  readonly stringifyTypeName = (item: Type): string => item.name.shortDisplayName;

  @Input()
  schema: Schema;

  @Input()
  label: string = "Select a type";

  @Input()
  additionalTypes: Type[] = [];


  selectedType: Type;


  @Output()
  selectedTypeChanged = new EventEmitter<SchemaMember>();

  get displayTypes(): Type[] {
    const additionalTypes = this.additionalTypes || [];
    return additionalTypes.concat(this.schema?.types || [])
      .filter(t => !t.fullyQualifiedName.startsWith("io.vyne")
        && !t.fullyQualifiedName.startsWith("lang.taxi")
        && !t.fullyQualifiedName.startsWith("taxi.stdlib")
        && !t.fullyQualifiedName.startsWith("vyne.vyneQl")
        && !t.fullyQualifiedName.startsWith("Anonymous")
      );
  }

  handleSelectedTypeChanged($event: Type) {
    this.selectedTypeChanged.emit(SchemaMember.fromType($event))
  }

}

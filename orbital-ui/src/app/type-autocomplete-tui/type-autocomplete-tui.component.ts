import {
  ChangeDetectionStrategy,
  Component, computed,
  EventEmitter,
  input,
  Input,
  InputSignal, Optional,
  Output, Signal, SkipSelf,
} from '@angular/core';
import {ControlContainer} from '@angular/forms';
import {Schema, SchemaMember, Type} from '../services/schema';

@Component({
  selector: 'app-type-autocomplete-tui',
  styleUrls: ['./type-autocomplete-tui.component.scss'],
  // Need this to be optional for when the ControlContainer (ie. ngForm) doesn't wrap
  // this component and Angular complains about injection issues
  viewProviders: [
    {
      provide: ControlContainer,
      useFactory: (controlContainer: ControlContainer | null) => controlContainer,
      deps: [[new Optional(), new SkipSelf(), ControlContainer]]
    }
  ],
  template: `
    <tui-combo-box
      class="type-input"
      [stringify]="stringifyTypeName"
      [tuiTextfieldSize]="size"
      [tuiTextfieldLabelOutside]="true"
      [valueContent]="value"
      [(ngModel)]="selectedType"
      (ngModelChange)="handleSelectedTypeChanged($event)"
      name="typeName"
      [tuiTextfieldCleaner]="true"
      [required]="isRequired"
      [disabled]="isDisabled"
    >
      {{ label }}
      <span *ngIf="isRequired" class="tui-required"></span>
      <input tuiTextfieldLegacy [placeholder]="label"/>
      <ng-template #value let-item>
        <div class="type-option">
          <span class="type-name">{{ item.name.shortDisplayName }}</span>
          <span class="mono-badge small" *ngIf="item.name.namespace">{{ item.name.namespace }}</span>
        </div>
      </ng-template>
      <ng-template tuiDataList>
        <tui-opt-group label="New type(s)">
          <tui-data-list *ngFor="let item of this.additionalTypes | tuiFilterByInput"
                         [size]="size">
            <button tuiOption [value]="item">
              <div class="type-option">
                <span class="type-name">{{ item.name.shortDisplayName }}</span>
                <span class="mono-badge small" *ngIf="item.name.namespace">{{ item.name.namespace }}</span>
              </div>
            </button>
          </tui-data-list>
        </tui-opt-group>
        <tui-opt-group label="Existing types">
          <tui-data-list *ngFor="let item of displayTypes() | tuiFilterByInput" [size]="size">
            <button tuiOption [value]="item">
              <div class="type-option">
                <span class="type-name">{{ item.name.shortDisplayName }}</span>
                <span class="mono-badge small" *ngIf="item.name.namespace">{{ item.name.namespace }}</span>
              </div>
            </button>
          </tui-data-list>
        </tui-opt-group>
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

  schema: InputSignal<Schema> = input();

  @Input()
  label: string = "Select a type";

  @Input()
  additionalTypes: Type[] = [];

  @Input()
  selectedType: Type;

  @Input()
  size: 's' | 'm' | 'l' = 's'

  @Input()
  isRequired: boolean

  @Input()
  isDisabled: boolean

  @Output()
  selectedTypeChanged = new EventEmitter<SchemaMember>();

  displayTypes: Signal<Type[]> = computed(() => {
    return (this.schema()?.types || [])
      .filter(t => !t.fullyQualifiedName.startsWith("io.vyne")
        && !t.fullyQualifiedName.startsWith("lang.taxi")
        && !t.fullyQualifiedName.startsWith("taxi.stdlib")
        && !t.fullyQualifiedName.startsWith("vyne.vyneQl")
        && !t.fullyQualifiedName.startsWith("Anonymous")
      );
  })

  handleSelectedTypeChanged($event: Type) {
    this.selectedTypeChanged.emit($event ? SchemaMember.fromType($event) : null)
  }

}

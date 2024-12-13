import {CdkFixedSizeVirtualScroll, CdkVirtualForOf, CdkVirtualScrollViewport} from '@angular/cdk/scrolling';
import {CommonModule} from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component, computed,
  EventEmitter,
  input,
  Input,
  InputSignal, Optional,
  Output, Signal, SkipSelf,
} from '@angular/core';
import {ControlContainer, FormsModule} from '@angular/forms';
import {TuiLet} from '@taiga-ui/cdk';
import {TuiDataListComponent, TuiOptGroup, TuiOption, TuiScrollable, TuiTextfield} from '@taiga-ui/core';
import {TuiDataListWrapper, TuiFilterByInputPipe} from '@taiga-ui/kit';
import {TuiComboBoxModule, TuiTextfieldControllerModule} from '@taiga-ui/legacy';
import {Schema, SchemaMember, Type} from '../services/schema';
import {IndexChange} from '../utils/index-change.directive';

@Component({
  selector: 'app-type-autocomplete-tui',
  styleUrls: ['./type-autocomplete-tui.component.scss'],
  // Need this to be optional for when the ControlContainer (ie. ngForm) doesn't wrap
  // this component and Angular complains about injection issues
  viewProviders: [
    {
      provide: ControlContainer,
      useFactory: (controlContainer: ControlContainer | null) => controlContainer,
      deps: [[new Optional(), new SkipSelf(), ControlContainer]],
    },
  ],
  standalone: true,
  imports: [
    TuiComboBoxModule,
    TuiTextfield,
    TuiTextfieldControllerModule,
    FormsModule,
    CommonModule,
    TuiDataListWrapper,
    TuiDataListComponent,
    TuiOptGroup,
    TuiFilterByInputPipe,
    TuiOption,
    CdkVirtualScrollViewport,
    TuiScrollable,
    CdkFixedSizeVirtualScroll,
    CdkVirtualForOf,
    TuiLet,
    IndexChange,
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
      <input tuiTextfieldLegacy [placeholder]="label" />
      <ng-template #value let-item>
        <div class="type-option">
          <span class="type-name">{{ item.name.shortDisplayName }}</span>
          <span class="mono-badge small" *ngIf="item.name.namespace">{{ item.name.namespace }}</span>
        </div>
      </ng-template>
      <ng-container *tuiDataList>
        <cdk-virtual-scroll-viewport
          *tuiLet="allTypes() | tuiFilterByInput as items"
          [appendOnly]="true"
          tuiScrollable
          class="scroll"
          [itemSize]="size === 's' ? 32 : size === 'm' ? 40 : 44"
          [style.height.px]="items.length * 32 + 8"
          (indexChange)="list.handleFocusLossIfNecessary()"
        >
          <tui-data-list #list [size]="size">
            <button
              *cdkVirtualFor="let item of items"
              tuiOption
              type="button"
              class="type-option-container"
              [value]="item"
            >
              <div class="type-option">
                <span class="mono-badge small" *ngIf="item.isNew">new</span>
                <span class="type-name">{{ item.name.shortDisplayName }}</span>
                <span class="mono-badge small" *ngIf="item.name.namespace">{{ item.name.namespace }}</span>
              </div>
            </button>
          </tui-data-list>
        </cdk-virtual-scroll-viewport>
      </ng-container>
      <!--              <tui-data-list-wrapper-->
      <!--                      *tuiDataList-->
      <!--                      [items]="types | tuiFilterByInputWith : stringifyTypeName"-->
      <!--                      [itemContent]="stringifyTypeName | tuiStringifyContent"></tui-data-list-wrapper>-->
    </tui-combo-box>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TypeAutocompleteTuiComponent {
  readonly stringifyTypeName = (item: Type): string => item.name.shortDisplayName;

  schema: InputSignal<Schema> = input();

  @Input()
  label: string = "Select a type";

  additionalTypes: InputSignal<Type[]> = input([]);

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

  private displayTypes: Signal<Type[]> = computed(() => {
    return (this.schema()?.types || [])
      .filter(t => !t.fullyQualifiedName.startsWith("io.vyne")
        && !t.fullyQualifiedName.startsWith("lang.taxi")
        && !t.fullyQualifiedName.startsWith("taxi.stdlib")
        && !t.fullyQualifiedName.startsWith("vyne.vyneQl")
        && !t.fullyQualifiedName.startsWith("Anonymous")
      );
  })

  allTypes: Signal<(Type | TypeWithIsNew)[]> = computed(() => {
    return [
      ...this.additionalTypes().map(type => ({
        ...type,
        isNew: true
      })),
      ...this.displayTypes()
    ];
  });

  handleSelectedTypeChanged($event: Type) {
    this.selectedTypeChanged.emit($event ? SchemaMember.fromType($event) : null)
  }

}

type TypeWithIsNew = Type & { isNew: boolean };

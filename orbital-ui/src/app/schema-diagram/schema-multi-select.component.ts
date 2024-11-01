import { TuiTextfieldControllerModule, TuiMultiSelectModule } from "@taiga-ui/legacy";
import {CommonModule} from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  model, Signal,
  signal,
  WritableSignal
} from '@angular/core';
import {FormsModule} from '@angular/forms';
import { TUI_DEFAULT_MATCHER, TuiHandler, tuiIsString, TuiContext, TuiLet } from '@taiga-ui/cdk';
import { TuiDataListWrapper } from '@taiga-ui/kit';
import {findSchemaMember, QualifiedName, Schema} from '../services/schema';

@Component({
  selector: 'app-schema-multi-select',
  styleUrls: ['./schema-multi-select.component.scss'],
  template: `
    <tui-multi-select
      *tuiLet="computedItems() as items"
      [(ngModel)]="selectedMembers"
      (searchChange)="onSearch($event)"
      [stringify]="computedStringify()"
      [tuiTextfieldCleaner]="true"
      placeholder="Select a member"
      tuiTextfieldSize="s"
      class="multi-select"
      rows="3"
    >
      Select a member
      <tui-data-list-wrapper
        *tuiDataList
        tuiMultiSelectGroup
        [itemContent]="itemContent"
        [items]="items"
      ></tui-data-list-wrapper>
    </tui-multi-select>
    <ng-template
      #itemContent
      let-data
    >
      <div class="template" *ngIf="schema()">
        {{ computedStringify().apply(this, [data]) }}
        <span
          *ngIf="findSchemaMember(schema(), data) as member"
          class="badge"
          [ngClass]="{'type': member.member.isScalar, 'model': !member.member.isScalar, 'service': member.kind !== 'TYPE'}"
        >
          @if (member.kind === 'TYPE' && member.member.isScalar) {
            Type
          } @else if (member.kind === 'TYPE') {
            Model
          } @else {
            {{member.member.serviceKind || 'Service'}}
          }
        </span>
      </div>
    </ng-template>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    TuiMultiSelectModule,
    TuiLet,
    TuiTextfieldControllerModule,
    TuiDataListWrapper,
  ],
  standalone: true
})
export class SchemaMultiSelectComponent {

  readonly schema = input.required<Schema>()
  readonly selectedMembers = model<string[]>();
  readonly schemaQualifiedNames = input.required<QualifiedName[]>()

  private readonly search: WritableSignal<string> = signal('')

  protected onSearch(search: string | null): void {
    this.search.set(search);
  }

  protected readonly computedItems = computed(() => {
    return this.schemaQualifiedNames()
      .filter(({name}) => TUI_DEFAULT_MATCHER(name, this.search() || ''))
      .map(({fullyQualifiedName}) => fullyQualifiedName)
  })

  protected readonly computedStringify: Signal<TuiHandler<TuiContext<string> | string, string>> = computed(() => {
    const nameMap = this.schemaQualifiedNames().reduce((map, obj) => {
      map.set(obj.fullyQualifiedName, obj.name);
      return map;
    }, new Map<string, string>());

    return (fullyQualifiedName: TuiContext<string> | string) => {
      return (tuiIsString(fullyQualifiedName) ?
        nameMap.get(fullyQualifiedName.split('@@')[0]) :
        nameMap.get(fullyQualifiedName.$implicit.split('@@')[0])) || 'Loading...'
    }
  })

  protected readonly findSchemaMember = findSchemaMember;
}

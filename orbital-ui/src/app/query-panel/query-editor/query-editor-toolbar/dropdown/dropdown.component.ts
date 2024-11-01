import {CommonModule} from '@angular/common';
import {ChangeDetectionStrategy, Component, input, Input, model} from '@angular/core';
import { TuiDataList, TuiDropdown, TuiIcon, TuiHint } from '@taiga-ui/core';

@Component({
  // TODO: this is a bit generic... but maybe that's ok?
  //       maybe we need this to be in a /components namespace instead of buried here?
  selector: 'app-dropdown',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule,
    TuiDataList,
    TuiHint,
    TuiDropdown,
    TuiIcon
  ],
  template: `
    <div
      tuiDropdownAlign="left"
      [tuiDropdown]="menuDropdown"
      [tuiDropdownOpen]="isMenuOpen()"
      (tuiDropdownOpenChange)="isMenuOpen.set($event)"
      [tuiDropdownEnabled]="!isDisabled"
      [tuiHint]="isDisabled ? hintWhenDisabled : null"
      tuiHintAppearance="dark"
      tuiHintDirection="top"
    >
      <a
        tuiLink
        class="dropdown-link"
        [class.is-open]="isMenuOpen()"
        [class.is-disabled]="isDisabled"
        [tuiHint]="hint"
        tuiHintAppearance="dark"
        tuiHintDirection="top"
      >
        {{value}}
        <img *ngIf="iconUrl" [src]="iconUrl">
        <tui-icon
          icon="@tui.chevron-down"
          class="dropdown-arrow"
          [class.dropdown-arrow_open]="isMenuOpen()"
        ></tui-icon>
      </a>
    </div>
    <ng-template
      #menuDropdown
      let-close="close"
    >
      <ng-content></ng-content>
    </ng-template>
  `,
  styleUrl: './dropdown.component.scss'
})
export class DropdownComponent {

  isMenuOpen = model(false);

  @Input()
  value: string // NOTE: only use value OR iconUrl, not both

  @Input()
  iconUrl: string

  @Input()
  isDisabled: boolean

  @Input()
  hint: string;

  @Input()
  hintWhenDisabled: string

}

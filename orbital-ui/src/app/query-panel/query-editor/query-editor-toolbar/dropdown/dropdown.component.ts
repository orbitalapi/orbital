import {CommonModule} from '@angular/common';
import {ChangeDetectionStrategy, Component, input, Input, model} from '@angular/core';
import {TuiDataListModule, TuiHintModule, TuiHostedDropdownModule, TuiSvgModule} from '@taiga-ui/core';

@Component({
  // TODO: this is a bit generic... but maybe that's ok?
  //       maybe we need this to be in a /components namespace instead of buried here?
  selector: 'app-dropdown',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule,
    TuiDataListModule,
    TuiHintModule,
    TuiHostedDropdownModule,
    TuiSvgModule
  ],
  template: `
    <tui-hosted-dropdown
      tuiDropdownAlign="left"
      [content]="menuDropdown"
      [open]="isMenuOpen()"
      (openChange)="isMenuOpen.set($event)"
      [canOpen]="!isDisabled"
      [tuiHint]="isDisabled ? hintWhenDisabled : null"
      tuiHintAppearance="onDark"
      tuiHintDirection="top"
    >
      <a
        tuiLink
        class="dropdown-link"
        [class.is-open]="isMenuOpen()"
        [class.is-disabled]="isDisabled"
        [tuiHint]="hint"
        tuiHintAppearance="onDark"
        tuiHintDirection="top"
      >
        {{value}}
        <img *ngIf="iconUrl" [src]="iconUrl">
        <tui-svg
          src="tuiIconChevronDown"
          class="dropdown-arrow"
          [class.dropdown-arrow_open]="isMenuOpen()"
        ></tui-svg>
      </a>
    </tui-hosted-dropdown>
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

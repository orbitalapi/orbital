import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TuiDialogContext } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { TuiButton } from '@taiga-ui/core';
import { HeaderComponentLayoutComponent } from '../header-component-layout/header-component-layout.component';
import { UiCustomisations } from '../../environments/ui-customisations';

export interface ConfigDisabledDialogData {
  settings: string[];
}

@Component({
  selector: 'app-config-disabled-dialog',
  template: `
    <app-header-component-layout
      title="This download option isn't available yet"
      [description]="description">
      <p>To enable this, update your server configuration and set the following to <code>true</code>:</p>
      <ul>
        <li *ngFor="let setting of context.data.settings"><code>{{ setting }}</code></li>
      </ul>
      <p>After updating, restart {{ UiCustomisations.productName }} and re-run your query.</p>
      <div class="button-row">
        <div class="spacer"></div>
        <button
          tuiButton
          type="button"
          size="m"
          appearance="primary"
          (click)="context.completeWith(undefined)"
        >
          Close
        </button>
      </div>
    </app-header-component-layout>
  `,
  styleUrls: ['./config-disabled-dialog.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    TuiButton,
    HeaderComponentLayoutComponent
  ]
})
export class ConfigDisabledDialogComponent {
  protected readonly UiCustomisations = UiCustomisations;
  readonly description = `${UiCustomisations.productName} needs to store additional data during query execution to support this feature, but the required settings are currently disabled.`;

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    public readonly context: TuiDialogContext<void, ConfigDisabledDialogData>
  ) {}
}

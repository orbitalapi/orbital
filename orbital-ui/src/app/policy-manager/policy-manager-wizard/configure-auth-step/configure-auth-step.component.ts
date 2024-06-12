import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TuiButtonModule } from '@taiga-ui/core';
import { TuiBlockStatusModule } from '@taiga-ui/layout';
import { UiCustomisations } from '../../../../environments/ui-customisations';

@Component({
  selector: 'app-configure-auth-step',
  standalone: true,
  imports: [CommonModule, TuiBlockStatusModule, TuiButtonModule],
  template: `
    <tui-block-status>
      <img tuiSlot="top" src="assets/img/illustrations/settings-28.svg">
      <h4>Configure authentication</h4>
      <p>
        To use data access policies, you need to configure authentication, so that {{ UiCustomisations.productName }}
        knows the user who is requesting data.
      </p>
      <p>
        Learn how to configure authentication in our
        <a class="link" [href]="UiCustomisations.docsLinks.authentication" target="_blank">docs</a>.
      </p>
      <a tuiButton appearance="secondary" [href]="UiCustomisations.docsLinks.authentication" target="_blank">Learn
        more</a>
    </tui-block-status>
  `,
  styleUrls: ['./configure-auth-step.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConfigureAuthStepComponent {

  protected readonly UiCustomisations = UiCustomisations;
}

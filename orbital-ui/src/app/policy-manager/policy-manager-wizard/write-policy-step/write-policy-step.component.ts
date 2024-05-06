import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TuiButtonModule } from '@taiga-ui/core';
import { TuiBlockStatusModule } from '@taiga-ui/layout';
import { UiCustomisations } from '../../../../environments/ui-customisations';

@Component({
  selector: 'app-write-policy-step',
  standalone: true,
  imports: [CommonModule, TuiButtonModule, TuiBlockStatusModule],
  template: `
    <tui-block-status>
      <img tuiSlot="top" src="assets/img/illustrations/settings-28.svg">
      <h4>Write a policy</h4>
      <p>Now that you've got a model setup, learn about writing a policy to make use of it.</p>
      <a tuiButton appearance="secondary" href="https://orbitalhq.com/docs/deploying/data-policies" target="_blank">Learn
        more</a>
    </tui-block-status>
  `,
  styleUrls: ['./write-policy-step.component.scss']
})
export class WritePolicyStepComponent {

  protected readonly UiCustomisations = UiCustomisations;
}

import { TuiBlockStatus } from "@taiga-ui/layout";
import { TuiButton } from "@taiga-ui/core";
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {RouterLink} from '@angular/router';
import { UiCustomisations } from '../../../../environments/ui-customisations';

@Component({
  selector: 'app-write-policy-step',
  standalone: true,
  imports: [CommonModule, TuiButton, TuiBlockStatus, RouterLink],
  template: `
    <tui-block-status>
      <img tuiSlot="top" src="assets/img/illustrations/settings-28.svg">
      <h4>Write a policy</h4>
      <p>Now that you've got a model setup, learn about writing a policy to make use of it.</p>
      <a tuiButton appearance="secondary" [href]="UiCustomisations.docsLinks.dataPolicies" target="_blank">
        Learn more
      </a>
      <p>Or put the new UserCredentials model you just created to use in the <a routerLink="/policies/editor">policy editor</a> now!</p>
    </tui-block-status>
  `,
  styleUrls: ['./write-policy-step.component.scss']
})
export class WritePolicyStepComponent {

  protected readonly UiCustomisations = UiCustomisations;
}

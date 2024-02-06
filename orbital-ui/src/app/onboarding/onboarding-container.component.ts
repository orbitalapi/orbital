import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TuiStepperModule } from '@taiga-ui/kit';
import { TuiLinkModule } from '@taiga-ui/core';
import { UiCustomisations } from '../../environments/ui-customisations';

@Component({
  selector: 'app-onboarding-container',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, TuiStepperModule, TuiLinkModule],
  templateUrl: './onboarding-container.component.html',
  styleUrls: ['./onboarding-container.component.scss']
})
export class OnboardingContainerComponent {
  readonly uiConfig = UiCustomisations;
}

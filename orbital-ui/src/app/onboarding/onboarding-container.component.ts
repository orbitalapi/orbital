import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import {
  ActivatedRoute,
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
  Scroll
} from '@angular/router';
import { TuiStepperModule } from '@taiga-ui/kit';
import { TuiLinkModule } from '@taiga-ui/core';
import { UiCustomisations } from '../../environments/ui-customisations';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';

@Component({
  selector: 'app-onboarding-container',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, TuiStepperModule, TuiLinkModule],
  templateUrl: './onboarding-container.component.html',
  styleUrls: ['./onboarding-container.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OnboardingContainerComponent {
  readonly uiConfig = UiCustomisations;

  readonly currentStepIndex$: Observable<number>;

  constructor(private router: Router, private activatedRoute: ActivatedRoute) {
    this.currentStepIndex$ = this.router.events.pipe(
      // NOTE: The Scroll event occurs here when the page first loads, not the NavigationEnd one
      filter((event) => event instanceof NavigationEnd || (event instanceof Scroll && event.routerEvent instanceof NavigationEnd)),
      map((event) => event instanceof Scroll ? event.routerEvent as NavigationEnd : event as NavigationEnd),
      map((event: NavigationEnd) => this.getStepIndex(event.url))
    );
  }

  private getStepIndex(url: string): number {
    switch (url) {
      case '/onboarding/project':
        return 0;
      case '/onboarding/data-source':
        return 1;
      case '/onboarding/data-source/configure':
        return 2;
      case '/onboarding/explore':
        return 3;
    }
  }

}

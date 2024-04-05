import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { TypesService } from '../services/types.service';
import { ChangeLogEntry } from '../changelog/changelog.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-landing-page-container',
  template: `
    <app-landing-page></app-landing-page>
  `,
  styleUrls: ['./landing-page-container.component.scss']
})
export class LandingPageContainerComponent {

  changelogEntries: ChangeLogEntry[] = [];

  constructor(typeService: TypesService, router: Router) {
    typeService.getTypes()
      .pipe(takeUntilDestroyed())
      .subscribe(type => {
        if (type.services.length === 0) {
          router.navigate(
            ['/onboarding'],
            {
              replaceUrl: true,
            }
          );
        }
      });
  }
}

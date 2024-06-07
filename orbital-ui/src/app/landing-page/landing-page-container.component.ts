import { Component } from '@angular/core';
import { ChangeLogEntry } from '../changelog/changelog.service';

/**
 * @deprecated superseded by the Dashboard
 */
@Component({
  selector: 'app-landing-page-container',
  template: `
    <app-landing-page></app-landing-page>
  `,
  styleUrls: ['./landing-page-container.component.scss']
})
export class LandingPageContainerComponent {

  changelogEntries: ChangeLogEntry[] = [];

}

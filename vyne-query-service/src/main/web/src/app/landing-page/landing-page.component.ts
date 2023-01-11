import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { QueryHistorySummary } from '../services/query.service';
import { Schema } from '../services/schema';
import { ChangeLogEntry } from '../changelog/changelog.service';
import { Observable } from 'rxjs';

export interface LandingPageCardConfig {
  title: string;
  emptyText: string;
  emptyActionLabel: string;
  emptyStateImage: string;
}

@Component({
  selector: 'app-landing-page',
  styleUrls: ['./landing-page.component.scss'],
  template: `
    <app-header-bar title="">
    </app-header-bar>
    <div class="container content-box schema-diagram-container">
      <app-schema-diagram title="Your services" [schema$]="schema$" displayedMembers="services"></app-schema-diagram>
    </div>


  `
})
export class LandingPageComponent {
  constructor(public readonly router: Router) {
  }

  @Input()
  schema$: Observable<Schema>;

}

import { Component } from '@angular/core';
import { TypesService } from '../services/types.service';
import { Schema } from '../services/schema';
import { ChangeLogEntry, ChangelogService } from '../changelog/changelog.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-landing-page-container',
  template: `
    <app-landing-page [schema$]="schema$"></app-landing-page>
  `,
  styleUrls: ['./landing-page-container.component.scss']
})
export class LandingPageContainerComponent {

  schema$: Observable<Schema>;
  changelogEntries: ChangeLogEntry[] = [];

  constructor(typeService: TypesService) {
    this.schema$ = typeService.getTypes();
  }

}

import { Component } from '@angular/core';
import { TypesService } from '../services/types.service';
import { Schema } from '../services/schema';
import { ChangeLogEntry, ChangelogService } from '../changelog/changelog.service';

@Component({
  selector: 'app-landing-page-container',
  template: `
    <app-landing-page [schema]="schema"
                      [changeLogEntries]="changelogEntries"></app-landing-page>
  `,
  styleUrls: ['./landing-page-container.component.scss']
})
export class LandingPageContainerComponent {

  schema: Schema;
  changelogEntries: ChangeLogEntry[] = [];

  constructor(typeService: TypesService, changelogService: ChangelogService) {
    typeService.getTypes()
      .subscribe(schema => {
        this.schema = schema;
        changelogService.getChangelog()
          .subscribe(changelog => this.changelogEntries = changelog);
      })

  }

}

import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { ParsedSource, VersionedSource } from '../services/schema';
import { AppInfoService, QueryServiceConfig } from '../services/app-info.service';
import { PackagesService, SourcePackageDescription } from '../package-viewer/packages.service';
import { ChangeLogEntry, ChangelogService } from '../changelog/changelog.service';
import { TypesService } from '../services/types.service';
import { ActivatedRoute, Router } from '@angular/router';
import { SchemaNotificationService } from '../services/schema-notification.service';

@Component({
  selector: 'app-schema-summary-view',
  template: `
    <div class="container">
      <div>
        <h1>Changelog</h1>
        <p>This is the changelog of all the data sources connected to Vyne.</p>
        <p>Click on a package on the left to view the schema for that package</p>
        <app-changelog-list [changeLogEntries]="changeLogEntries | async"></app-changelog-list>
      </div>

    </div>


    <div *ngIf="(schemas | async)?.length==0" class="empty-state">
      <img src="assets/img/getting-started.svg">
      <div>There's nothing registered yet. Schemas will appear here as services register, or you can add
        a new schema manually from the toolbar above.
      </div>
    </div>

  `,
  styleUrls: ['./schema-summary-view.component.scss']
})
export class SchemaSummaryViewComponent {

  schemas: Observable<ParsedSource[]>;

  config: QueryServiceConfig;
  changeLogEntries: Observable<ChangeLogEntry[]>

  constructor(private service: TypesService,
              private configService: AppInfoService,
              private router: Router,
              private changelogService: ChangelogService,
              private activatedRoute: ActivatedRoute,
              private schemaNotificationService: SchemaNotificationService) {
    this.loadSchemas();
    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .subscribe(() => {
        this.loadSchemas();
      });
    this.configService.getConfig().subscribe(result => this.config = result);
  }

  private loadSchemas() {
    this.schemas = this.service.getParsedSources();
    this.changeLogEntries = this.changelogService.getChangelog();
  }
}
